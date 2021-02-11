package org.jetbrains.tinygoplugin.services

import com.goide.util.GoExecutor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.GarbageCollector
import org.jetbrains.tinygoplugin.Scheduler
import org.jetbrains.tinygoplugin.TinyGoConfiguration
import java.nio.file.Paths

class TinyGoInfoExtractor(private val project: Project) {
    companion object {
        private val logger: Logger = Logger.getInstance(TinyGoInfoExtractor::class.java)
    }

    fun assembleTinyGoShellCommand(): GoExecutor {
        val executor = GoExecutor.`in`(project, null)
        val settings = TinyGoConfiguration.getInstance(project)
        val parameters = tinyGoArguments(settings)
        executor.withParameterString("info $parameters")
        executor.showNotifications(true, false)
        val tinyGoExec = Paths.get(
            Paths.get(settings.tinyGoSDKPath).toAbsolutePath().toString(),
            "bin",
            "tinygo"
        )
        executor.withExePath(tinyGoExec.toString())
        return executor
    }

    private fun tinyGoArguments(settings: TinyGoConfiguration): String {
        val parametersList = mutableListOf("-target", settings.targetPlatform)
        if (settings.scheduler != Scheduler.AUTO_DETECT) {
            parametersList.addAll(listOf("-scheduler", settings.scheduler.cmd))
        }
        if (settings.gc != GarbageCollector.AUTO_DETECT) {
            parametersList.addAll(listOf("-gc", settings.gc.cmd))
        }
        return parametersList.joinToString(" ", " ", " ")
    }

    fun extractTinyGoInfo(msg: String) {
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

        val settings = TinyGoConfiguration.getInstance(project)

        settings.goArch = goArch.groupValues[1]
        settings.goTags = tags.groupValues[1]
        settings.goOS = goOS.groupValues[1]
        settings.gc = GarbageCollector.valueOf(gc.groupValues[1].toUpperCase())
        settings.scheduler = Scheduler.valueOf(scheduler.groupValues[1].toUpperCase())

        logger.warn("extraction finished")
    }
}
