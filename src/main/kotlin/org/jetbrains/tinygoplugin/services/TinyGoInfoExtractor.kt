package org.jetbrains.tinygoplugin.services

import com.goide.GoOsManager
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.download.GoDownloadingSdk
import com.goide.util.GoExecutor
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.TinyGoExtractionFailureListener
import org.jetbrains.tinygoplugin.configuration.sendReloadLibrariesSignal
import org.jetbrains.tinygoplugin.sdk.TinyGoDownloadingSdk
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured
import org.jetbrains.tinygoplugin.sdk.osManager
import java.time.Duration
import java.util.Locale
import java.util.function.BiConsumer
import kotlin.jvm.Throws

private const val GO_NOT_CONFIGURED_MESSAGE = "notifications.tinygoSDK.goSDKNotConfigured"
private const val TINYGO_TARGET_PLATFORM_NOT_SET = "notifications.tinygoSDK.tinyGoTargetNotSet"
private const val DETECTION_TITLE = "notifications.tinygoSDK.detection.title"
private const val DETECTION_INDICATOR_TEXT = "notifications.tinygoSDK.detection.indicatorText"
private const val DETECTION_ERROR_MESSAGE = "notifications.tinygoSDK.detection.errorMessage"

fun TinyGoConfiguration.extractTinyGoInfo(msg: String) {
    val tagPattern = Regex("""build tags:\s+((.|\n)+?(?=\n, garbage collector))""")
    val goArchPattern = Regex("""GOARCH:\s+(.+)\n""")
    val goOSPattern = Regex("""GOOS:\s+(.+)\n""")
    val gcPattern = Regex("""garbage collector:\s+(.+)\n""")
    val schedulerPattern = Regex("""scheduler:\s+(.+)\n""")
    val cachedGoRootPattern = Regex("""cached GOROOT:\s+((.|\n)+?(?=\n]))""")

    try {
        val tags = tagPattern.findFirst(msg)
        val goArch = goArchPattern.findFirst(msg)
        val goOS = goOSPattern.findFirst(msg)
        val gc = gcPattern.findFirst(msg)
        val scheduler = schedulerPattern.findFirst(msg)
        val cachedGoRoot = cachedGoRootPattern.findFirst(msg)

        this.goArch = goArch.firstGroup()
        this.goTags = tags.firstGroup().eraseLineBreaks()
        this.goOS = goOS.firstGroup()
        this.gc = GarbageCollector.valueOf(gc.firstGroup().uppercase(Locale.getDefault()))
        this.scheduler = Scheduler.valueOf(scheduler.firstGroup().uppercase(Locale.getDefault()))
        this.cachedGoRoot = GoSdk.fromUrl(VfsUtil.pathToUrl(cachedGoRoot.firstGroup().eraseLineBreaks()))

        TinyGoInfoExtractor.logger.info("extraction finished")
    } catch (e: NoSuchElementException) {
        TinyGoInfoExtractor.logger.error(
            "error while extracting parameters from tinygo command output", e,
            "process output: \"$msg\"\n"
        )
    }
}

@Throws(NoSuchElementException::class)
private fun Regex.findFirst(input: CharSequence): MatchResult = findAll(input).first()
private fun MatchResult.firstGroup(): String = groupValues[1]
private fun String.eraseLineBreaks(): String = replace(Regex("\n((,)? )?"), "")

class TinyGoExecutable(private val project: Project) {
    fun execute(
        sdkRoot: String,
        arguments: List<String>,
        failureListener: TinyGoExtractionFailureListener? = null,
        onFinish: BiConsumer<in GoExecutor.ExecutionResult?, in String>,
    ) {
        val processHistory = GoHistoryProcessListener()
        val tinyGoExec = osManager.executablePath(sdkRoot)
        val executor = GoExecutor.`in`(project, null)
            .withExePath(tinyGoExec)
            .withParameters(arguments)
            .showNotifications(true, false)
        if (GoOsManager.isWindows()) {
            executor.withConsoleMode()
        }
        executor.executeWithProgress(true, true, processHistory, null) {
            val processOutput = processHistory.output.toString()
            if (it.status.ordinal == 0) {
                onFinish.accept(it, processOutput)
            } else {
                val incompatibleVersionErrorMessage = generateMessageIfVersionErrorFound(project, processOutput)
                val errorMessage =
                    if (incompatibleVersionErrorMessage != null) {
                        incompatibleVersionErrorMessage
                    } else {
                        val detectionErrorMessage = TinyGoBundle.message(DETECTION_ERROR_MESSAGE)
                        TinyGoInfoExtractor.logger.error(detectionErrorMessage, processOutput)
                        detectionErrorMessage
                    }
                failureListener?.onExtractionFailure()
                notifyTinyGoNotConfigured(project, errorMessage)
            }
        }
    }
}

@Service
class TinyGoInfoExtractor(private val project: Project) {
    companion object {
        val logger: Logger = logger<TinyGoInfoExtractor>()
    }

    private val executor = TinyGoExecutable(project)

    private fun tinyGoExtractionArguments(settings: TinyGoConfiguration): List<String> {
        return listOf("info") + tinyGoArguments(settings)
    }

    fun extractTinyGoInfo(
        settings: TinyGoConfiguration,
        failureListener: TinyGoExtractionFailureListener? = null,
        onFinish: BiConsumer<in GoExecutor.ExecutionResult?, in String>,
    ) {
        val currentGoSdk = project.service<GoSdkService>().getSdk(null)
        if (currentGoSdk == GoSdk.NULL) {
            notifyTinyGoNotConfigured(
                project,
                TinyGoBundle.message(GO_NOT_CONFIGURED_MESSAGE)
            )
            logger.debug(GO_NOT_CONFIGURED_MESSAGE)
            return
        }
        if (settings.targetPlatform.isEmpty()) {
            notifyTinyGoNotConfigured(
                project,
                TinyGoBundle.message(TINYGO_TARGET_PLATFORM_NOT_SET)
            )
            logger.debug(TINYGO_TARGET_PLATFORM_NOT_SET)
            return
        }
        val detectingTask = object : Task.Backgroundable(project, TinyGoBundle.message(DETECTION_TITLE)) {
            override fun run(indicator: ProgressIndicator) {
                synchronized(project) {
                    if (currentGoSdk is GoDownloadingSdk) {
                        logger.debug("Waiting until Go SDK will be downloaded")
                        indicator.isIndeterminate = true
                        indicator.text2 = TinyGoBundle.message(DETECTION_INDICATOR_TEXT)
                        while (project.service<GoSdkService>().getSdk(null) is GoDownloadingSdk) {
                            Thread.sleep(Duration.ofSeconds(1).toMillis())
                        }
                    }
                    logger.debug("Go SDK present")
                    var reloadNeeded = false
                    if (settings.sdk is TinyGoDownloadingSdk) {
                        logger.debug("Waiting until TinyGo SDK will be downloaded. Explicit library reload needed")
                        indicator.isIndeterminate = true
                        indicator.text2 = TinyGoBundle.message(DETECTION_INDICATOR_TEXT)
                        while (settings.sdk is TinyGoDownloadingSdk) {
                            Thread.sleep(Duration.ofSeconds(1).toMillis())
                        }
                        reloadNeeded = true
                    }
                    logger.debug("TinyGo SDK present")
                    executor.execute(
                        settings.sdk.sdkRoot!!.path,
                        tinyGoExtractionArguments(settings),
                        failureListener,
                        onFinish
                    )
                    if (reloadNeeded) {
                        logger.debug("Explicit library reload needed. Sending reload signal")
                        sendReloadLibrariesSignal(project)
                    }
                }
            }
        }
        logger.debug("Waiting for TinyGo parameters extraction task")
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(detectingTask, BackgroundableProcessIndicator(detectingTask))
        logger.debug("TinyGo parameters extraction task finished")
    }
}
