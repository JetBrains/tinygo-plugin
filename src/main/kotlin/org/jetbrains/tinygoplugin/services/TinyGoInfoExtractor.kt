package org.jetbrains.tinygoplugin.services

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.sdk.download.GoDownloadingSdk
import com.goide.util.GoExecutor
import com.goide.util.GoHistoryProcessListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured
import org.jetbrains.tinygoplugin.sdk.osManager
import java.time.Duration

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

internal class TinyGoInfoExtractor(private val project: Project) {
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoInfoExtractor::class.java)
        const val GO_NOT_CONFIGURED_MESSAGE = "notifications.tinygoSDK.goSDKNotConfigured"
        const val DETECTION_TITLE = "notifications.tinygoSDK.detection.title"
        const val DETECTION_INDICATOR_TEXT = "notifications.tinygoSDK.detection.indicatorText"
        const val DETECTION_FAIL_MESSAGE = "notifications.tinygoSDK.detection.failMessage"
        const val DETECTION_ERROR_MESSAGE = "notifications.tinygoSDK.detection.errorMessage"
    }

    private fun assembleTinyGoShellCommand(settings: TinyGoConfiguration): GoExecutor {
        val executor = GoExecutor.`in`(project, null)
        val parameters = tinyGoArguments(settings)
        executor.withParameters(parameters)
        executor.showNotifications(true, false)
        val tinyGoExec = osManager.executablePath(settings.tinyGoSDKPath)
        executor.withExePath(tinyGoExec)
        return executor
    }

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
        processHistory: GoHistoryProcessListener,
        onFinish: Consumer<in GoExecutor.ExecutionResult?>,
    ) {
        val currentGoSdk = GoSdkService.getInstance(project).getSdk(null)
        if (currentGoSdk == GoSdk.NULL) {
            notifyTinyGoNotConfigured(
                project,
                TinyGoBundle.message(GO_NOT_CONFIGURED_MESSAGE),
                NotificationType.WARNING,
                true
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
                val executor = assembleTinyGoShellCommand(settings)
                executor.executeWithProgress(true, true, processHistory, null) {
                    if (it.status.ordinal == 0) {
                        onFinish.consume(it)
                    } else {
                        notifyTinyGoNotConfigured(
                            project,
                            TinyGoBundle.message(DETECTION_FAIL_MESSAGE),
                            NotificationType.ERROR,
                            true
                        )
                        logger.error(TinyGoBundle.message(DETECTION_ERROR_MESSAGE), processHistory.output.toString())
                    }
                }
            }
        }
        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(detectingTask, BackgroundableProcessIndicator(detectingTask))
    }
}
