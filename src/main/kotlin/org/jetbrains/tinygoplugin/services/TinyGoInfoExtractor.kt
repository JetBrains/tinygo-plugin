package org.jetbrains.tinygoplugin.services

import com.goide.util.GoExecutor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.TinyGoConfiguration
import java.nio.file.Paths

class TinyGoInfoExtractor(private val project: Project) {
    companion object {
        private val logger: Logger = Logger.getInstance(TinyGoInfoExtractor::class.java)
    }

    fun assembleTinyGoShellCommand(): GoExecutor {
        val executor = GoExecutor.`in`(project, null)
        val settings = TinyGoConfiguration.getInstance(project)
        /* ktlint-disable*/
        val parameters = listOf(
            "-target", settings.targetPlatform,
            "-scheduler", settings.scheduler.cmd,
            "-gc", settings.gc.cmd
        )
            .joinToString(" ", " ", " ")
        /* ktlint-enable */
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

    fun extractTinyGoInfo(msg: String) {
        val tagPattern = Regex("""build tags:\s+(.+)\n""")
        val goArchPattern = Regex("""GOARCH:\s+(.+)\n""")
        val goOSPattern = Regex("""GOOS:\s+(.+)\n""")

        val tags = tagPattern.findAll(msg).first()
        val goArch = goArchPattern.findAll(msg).first()
        val goOS = goOSPattern.findAll(msg).first()

        val settings = TinyGoConfiguration.getInstance(project)

        settings.goArch = goArch.groupValues[1]
        settings.goTags = tags.groupValues[1]
        settings.goOS = goOS.groupValues[1]

        logger.warn("extraction finished")
    }
}
