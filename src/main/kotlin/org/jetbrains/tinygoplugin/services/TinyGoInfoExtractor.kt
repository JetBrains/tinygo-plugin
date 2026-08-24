package org.jetbrains.tinygoplugin.services

import com.goide.GoOsManager
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.download.GoDownloadingSdk
import com.goide.util.GoExecutor
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.EnvironmentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.TinyGoExtractionFailureListener
import org.jetbrains.tinygoplugin.sdk.TinyGoDownloadingSdk
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured
import org.jetbrains.tinygoplugin.sdk.osManager
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.jvm.Throws
import kotlin.time.Duration.Companion.seconds

private const val GO_NOT_CONFIGURED_MESSAGE = "notifications.tinygoSDK.goSDKNotConfigured"
private const val TINYGO_TARGET_PLATFORM_NOT_SET = "notifications.tinygoSDK.tinyGoTargetNotSet"
private const val DETECTION_TITLE = "notifications.tinygoSDK.detection.title"
private const val WAIT_GO_TITLE = "notifications.tinygoSDK.detection.wait.go.indicatorText"
private const val DETECTION_ERROR_MESSAGE = "notifications.tinygoSDK.detection.errorMessage"

@Suppress("UnstableApiUsage")
suspend fun TinyGoConfiguration.extractTinyGoInfo(msg: String) {
    val tagPattern = Regex("""build tags:\s+([^\r\n]+)""")
    val goArchPattern = Regex("""GOARCH:\s+([^\r\n]+)""")
    val goOSPattern = Regex("""GOOS:\s+([^\r\n]+)""")
    val gcPattern = Regex("""garbage collector:\s+([^\r\n]+)""")
    val schedulerPattern = Regex("""scheduler:\s+([^\r\n]+)""")
    val cachedGoRootPattern = Regex("""cached GOROOT:\s+([^\r\n\]]+)""")

    try {
        val tags = tagPattern.findFirst(msg)
        val goArch = goArchPattern.findFirst(msg)
        val goOS = goOSPattern.findFirst(msg)
        val gc = gcPattern.findFirst(msg)
        val scheduler = schedulerPattern.findFirst(msg)
        val cachedGoRoot = cachedGoRootPattern.findFirst(msg)

        val cachedGoRootSdk = readAction {
            GoSdk.fromUrl(VfsUtil.pathToUrl(cachedGoRoot.firstGroup()))
        }
        writeAction {
            this.goArch = goArch.firstGroup()
            this.goTags = tags.firstGroup()
            this.goOS = goOS.firstGroup()
            this.gc = GarbageCollector.valueOf(gc.firstGroup().uppercase(Locale.getDefault()))
            this.scheduler = Scheduler.valueOf(scheduler.firstGroup().uppercase(Locale.getDefault()))
            this.cachedGoRoot = cachedGoRootSdk
        }

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
private fun MatchResult.firstGroup(): String = groupValues[1].trim()

data class TinyGoCommandResult(
    val execution: GoExecutor.ExecutionResult,
    val stdout: String,
    val stderr: String,
    val output: String,
) {
    val isSuccessful: Boolean
        get() = execution.status == GoExecutor.ExecutionResult.Status.SUCCEEDED
}

class TinyGoExecutable(private val project: Project) {
    private suspend fun createExecutor(
        sdkRoot: VirtualFile?,
        arguments: List<String>,
        showErrors: Boolean,
    ): GoExecutor? {
        val (tinyGoExec, goBinPath) = readAction {
            val executable = osManager.executableVFile(sdkRoot)
            val goSdkRoot = project.service<GoSdkService>().getSdk(null).sdkRoot
            executable to goSdkRoot?.findChild("bin")?.path
        }
        if (tinyGoExec == null) return null

        val pathVariable = if (GoOsManager.isWindows()) "Path" else "PATH"
        val parentPath = EnvironmentUtil.getValue(pathVariable)
            ?: EnvironmentUtil.getValue("PATH")
            ?: ""
        val path = listOfNotNull(goBinPath, parentPath.takeIf(String::isNotEmpty))
            .joinToString(File.pathSeparator)

        return GoExecutor.`in`(project, null)
            .withExePath(tinyGoExec.path)
            .withParameters(arguments)
            .withExtraEnvironment(
                mapOf(
                    pathVariable to path,
                    "GOTOOLCHAIN" to "local",
                )
            )
            .showNotifications(showErrors, false)
            .withPtyEnabled(false)
            .also {
                if (GoOsManager.isWindows()) {
                    it.withConsoleMode()
                }
            }
    }

    suspend fun execute(
        sdkRoot: VirtualFile?,
        arguments: List<String>,
        showErrors: Boolean = false,
    ): TinyGoCommandResult? {
        val processHistory = GoHistoryProcessListener()
        val executor = createExecutor(sdkRoot, arguments, showErrors)
            ?.withProcessListener(processHistory)
            ?: return null
        return suspendCancellableCoroutine { continuation ->
            executor.executeWithProgress(true, showErrors, null, null) { execution ->
                if (continuation.isActive) {
                    continuation.resume(
                        TinyGoCommandResult(
                            execution = execution,
                            stdout = processHistory.stdout.joinToString(""),
                            stderr = processHistory.stderr.joinToString(""),
                            output = processHistory.output.joinToString(""),
                        )
                    )
                }
            }
        }
    }
}

@Service(Service.Level.PROJECT)
class TinyGoInfoExtractor(private val project: Project) {
    companion object {
        val logger: Logger = logger<TinyGoInfoExtractor>()
    }

    private val executor = TinyGoExecutable(project)

    private fun tinyGoExtractionArguments(settings: TinyGoConfiguration): List<String> {
        return listOf("info") + tinyGoArguments(settings)
    }

    suspend fun extractTinyGoInfo(
        settings: TinyGoConfiguration,
        failureListener: TinyGoExtractionFailureListener? = null,
    ): String? {
        val currentGoSdk = project.service<GoSdkService>().getSdk(null)
        val canExtract = when {
            currentGoSdk == GoSdk.NULL -> {
                notifyTinyGoNotConfigured(project, TinyGoBundle.message(GO_NOT_CONFIGURED_MESSAGE))
                logger.debug(GO_NOT_CONFIGURED_MESSAGE)
                false
            }
            settings.targetPlatform.isEmpty() -> {
                notifyTinyGoNotConfigured(project, TinyGoBundle.message(TINYGO_TARGET_PLATFORM_NOT_SET))
                logger.debug(TINYGO_TARGET_PLATFORM_NOT_SET)
                false
            }
            settings.sdk is TinyGoDownloadingSdk -> {
                logger.debug("Waiting for TinyGo SDK download to finish before extracting parameters")
                false
            }
            else -> true
        }
        if (!canExtract) return null
        logger.debug("Waiting for TinyGo parameters extraction task")
        val output = withBackgroundProgress(project, TinyGoBundle.message(DETECTION_TITLE), cancellable = true) {
            if (currentGoSdk is GoDownloadingSdk) {
                logger.debug("Waiting until Go SDK will be downloaded")
                withProgressText(TinyGoBundle.message(WAIT_GO_TITLE)) {
                    while (project.service<GoSdkService>().getSdk(null) is GoDownloadingSdk) {
                        delay(1.seconds.inWholeMilliseconds)
                    }
                }
            }
            logger.debug("Go SDK present")
            val result = executor.execute(
                settings.sdk.sdkRoot,
                tinyGoExtractionArguments(settings),
                showErrors = true,
            ) ?: return@withBackgroundProgress null
            if (result.isSuccessful) {
                result.stdout
            } else {
                reportFailure(result.output, failureListener)
                null
            }
        }
        logger.debug("TinyGo parameters extraction task finished")
        return output
    }

    private suspend fun reportFailure(
        processOutput: String,
        failureListener: TinyGoExtractionFailureListener?,
    ) {
        withContext(Dispatchers.EDT) {
            val incompatibleVersionErrorMessage = generateMessageIfVersionErrorFound(project, processOutput)
            val errorMessage = incompatibleVersionErrorMessage ?: TinyGoBundle.message(DETECTION_ERROR_MESSAGE).also {
                logger.error(it, processOutput)
            }
            failureListener?.onExtractionFailure()
            notifyTinyGoNotConfigured(project, errorMessage)
        }
    }
}
