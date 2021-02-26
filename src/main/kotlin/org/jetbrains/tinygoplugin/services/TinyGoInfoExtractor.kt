package org.jetbrains.tinygoplugin.services

import com.goide.util.GoExecutor
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import java.nio.file.Paths

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
    }

    fun assembleTinyGoShellCommand(settings: TinyGoConfiguration): GoExecutor {
        val executor = GoExecutor.`in`(project, null)
        val parameters = tinyGoArguments(settings)
        executor.withParameters(parameters)
        executor.showNotifications(true, false)
        val tinyGoExec = Paths.get(
            Paths.get(settings.tinyGoSDKPath).toAbsolutePath().toString(),
            "bin",
            "tinygo"
        )
        executor.withExePath(tinyGoExec.toString())
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
        val executor = assembleTinyGoShellCommand(settings)
        executor.executeWithProgress(true, true, processHistory, null, onFinish)
    }
}
