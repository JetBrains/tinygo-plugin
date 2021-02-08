package org.jetbrains.tinygoplugin.services

import com.goide.util.GoExecutor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.TinyGoSettingsUI
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.nio.file.Paths
import javax.swing.JComponent

class TinyGoSettingsService(private val project: Project) : Configurable, ActionListener {
    companion object {
        private val logger: Logger = Logger.getInstance(TinyGoSettingsService::class.java)
    }

    lateinit var settingsUI: TinyGoSettingsUI

    private fun setSettingsToUI(settings: TinyGoConfiguration) {
        settingsUI.targetPlatform = settings.targetPlatform
        settingsUI.tinyGoPath = Paths.get(settings.tinyGoSDKPath).toAbsolutePath().toString()
        settingsUI.garbageCollector = settings.gc
        settingsUI.scheduler = settings.scheduler
    }

    override fun createComponent(): JComponent {
        settingsUI = TinyGoSettingsUI(this)
        val settings = TinyGoConfiguration.getInstance(project)
        setSettingsToUI(settings)
        return settingsUI.mainPanel
    }

    override fun isModified(): Boolean {
        fun compareFile(file: String, str: String): Boolean = Paths.get(file) == Paths.get(str)
        val settings = TinyGoConfiguration.getInstance(project)

        return listOf(
            settingsUI.targetPlatform != settings.targetPlatform,
            !compareFile(str = settingsUI.tinyGoPath, file = settings.tinyGoSDKPath),
            settingsUI.garbageCollector != settings.gc,
            settingsUI.scheduler != settings.scheduler
        ).any { it }
    }

    override fun apply() {
        val settings = TinyGoConfiguration.getInstance(project)
        settings.targetPlatform = settingsUI.targetPlatform
        settings.tinyGoSDKPath = settingsUI.tinyGoPath
        settings.scheduler = settingsUI.scheduler
        settings.gc = settingsUI.garbageCollector
    }

    override fun getDisplayName(): String = "TinyGoPlugin"
    override fun actionPerformed(e: ActionEvent?) {
        logger.debug("Starting tinygo tags detection")
        settingsUI.setDetectionInProgress()
        val executor = GoExecutor.`in`(project, null)
        val settings = TinyGoConfiguration.getInstance(project)
        val parameters = listOf("-target", settings.targetPlatform,
            "-scheduler", settings.scheduler.cmd, " -gc", settings.gc.cmd)
            .joinToString(" ")
        executor.withParameterString("info $parameters")
        executor.showNotifications(true, false)
        val tinyGoExec = Paths.get(
            Paths.get(settings.tinyGoSDKPath).toAbsolutePath().toString(), "bin", "tinygo")
        logger.debug("Tinygo path: $tinyGoExec")
        logger.debug("Tinygo parameters: $parameters")
        executor.withExePath(tinyGoExec.toString())
        executor.executeWithProgress(true, true) { result: GoExecutor.ExecutionResult ->
            logger.warn("${result.status}")
            logger.warn("${result.message}")
            val output = result.message!!
            logger.warn(output)
            extractTinyGoInfo(output)
            settingsUI.updateTinyGoOutput(settings.goArch, settings.goTags)
        }
    }

    private fun extractTinyGoInfo(msg: String) {
        val tagPattern = Regex("""^build tags:\s+(.+)${'$'})""")
        val goarchPattern = Regex("""^GOARCH:\s+(.+)${'$'}""")

        val tags = tagPattern.find(msg)!!
        val goarch = goarchPattern.find(msg)!!

        val settings = TinyGoConfiguration.getInstance(project)

        settings.goArch = goarch.groupValues[0]
        settings.goTags = tags.groupValues[0]
        logger.warn("extraction finished")
    }
}
