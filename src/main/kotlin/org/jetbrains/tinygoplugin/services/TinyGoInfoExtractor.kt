package org.jetbrains.tinygoplugin.services

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.download.GoDownloadingSdk
import com.goide.util.GoExecutor
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured
import org.jetbrains.tinygoplugin.sdk.osManager
import java.time.Duration
import java.util.function.BiConsumer

private const val GO_NOT_CONFIGURED_MESSAGE = "notifications.tinygoSDK.goSDKNotConfigured"
private const val DETECTION_TITLE = "notifications.tinygoSDK.detection.title"
private const val DETECTION_INDICATOR_TEXT = "notifications.tinygoSDK.detection.indicatorText"
private const val DETECTION_FAIL_MESSAGE = "notifications.tinygoSDK.detection.failMessage"
private const val DETECTION_ERROR_MESSAGE = "notifications.tinygoSDK.detection.errorMessage"

fun TinyGoConfiguration.extractTinyGoInfo(msg: String) {
    val tagPattern = Regex("""build tags:\s+(.+)\n""")
    val goArchPattern = Regex("""GOARCH:\s+(.+)\n""")
    val goOSPattern = Regex("""GOOS:\s+(.+)\n""")
    val gcPattern = Regex("""garbage collector:\s+(.+)\n""")
    val schedulerPattern = Regex("""scheduler:\s+(.+)\n""")

    val tags = tagPattern.findAll(msg).first()
    val goArch = goArchPattern.findAll(msg).first()
    val goOS = goOSPattern.findAll(msg).first()
    val gc = gcPattern.findAll(msg).first()
    val scheduler = schedulerPattern.findAll(msg).first()

    this.goArch = goArch.groupValues[1]
    this.goTags = tags.groupValues[1]
    this.goOS = goOS.groupValues[1]
    this.gc = GarbageCollector.valueOf(gc.groupValues[1].toUpperCase())
    this.scheduler = Scheduler.valueOf(scheduler.groupValues[1].toUpperCase())

    TinyGoInfoExtractor.logger.warn("extraction finished")
}

class TinyGoExecutable(private val project: Project) {
    fun execute(
        sdkRoot: String,
        arguments: List<String>,
        onFinish: BiConsumer<in GoExecutor.ExecutionResult?, in String>,
    ) {
        val processHistory = GoHistoryProcessListener()
        val tinyGoExec = osManager.executablePath(sdkRoot)
        val executor = GoExecutor.`in`(project, null)
            .withExePath(tinyGoExec)
            .withParameters(arguments)
            .showNotifications(true, false)
        executor.executeWithProgress(true, true, processHistory, null) {
            if (it.status.ordinal == 0) {
                onFinish.accept(it, processHistory.output.joinToString(""))
            } else {
                notifyTinyGoNotConfigured(
                    project,
                    TinyGoBundle.message(DETECTION_FAIL_MESSAGE)
                )
                TinyGoInfoExtractor.logger.error(
                    TinyGoBundle.message(DETECTION_ERROR_MESSAGE),
                    processHistory.output.toString()
                )
            }
        }
    }
}

class TinyGoInfoExtractor(private val project: Project) {
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoInfoExtractor::class.java)
    }

    private val executor = TinyGoExecutable(project)

    private fun tinyGoArguments(settings: TinyGoConfiguration): List<String> {
        val parameters = mutableListOf("info", "-target", settings.targetPlatform)
        if (settings.scheduler != Scheduler.AUTO_DETECT) {
            parameters.addAll(listOf("-scheduler", settings.scheduler.cmd))
        }
        if (settings.gc != GarbageCollector.AUTO_DETECT) {
            parameters.addAll(listOf("-gc", settings.gc.cmd))
        }
        return parameters
    }

    fun extractTinyGoInfo(
        settings: TinyGoConfiguration,
        onFinish: BiConsumer<in GoExecutor.ExecutionResult?, in String>,
    ) {
        val currentGoSdk = GoSdkService.getInstance(project).getSdk(null)
        if (currentGoSdk == GoSdk.NULL) {
            notifyTinyGoNotConfigured(
                project,
                TinyGoBundle.message(GO_NOT_CONFIGURED_MESSAGE)
            )
            return
        }
        val detectingTask = object : Task.Backgroundable(project, TinyGoBundle.message(DETECTION_TITLE)) {
            override fun run(indicator: ProgressIndicator) {
                if (currentGoSdk is GoDownloadingSdk) {
                    indicator.isIndeterminate = true
                    indicator.text2 = TinyGoBundle.message(DETECTION_INDICATOR_TEXT)
                    while (GoSdkService.getInstance(project).getSdk(null) is GoDownloadingSdk) {
                        Thread.sleep(Duration.ofSeconds(1).toMillis())
                    }
                }
                executor.execute(settings.tinyGoSDKPath.executable!!.path, tinyGoArguments(settings), onFinish)
            }
        }
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(detectingTask, BackgroundableProcessIndicator(detectingTask))
    }
}
