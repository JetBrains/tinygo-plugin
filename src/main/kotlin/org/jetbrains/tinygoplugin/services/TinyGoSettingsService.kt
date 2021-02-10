package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
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
    private val infoExtractor = TinyGoInfoExtractor(project)

    private fun setSettingsToUI(settings: TinyGoConfiguration) {
        settingsUI.targetPlatform = settings.targetPlatform
        settingsUI.tinyGoPath = Paths.get(settings.tinyGoSDKPath).toAbsolutePath().toString()
        settingsUI.garbageCollector = settings.gc
        settingsUI.scheduler = settings.scheduler
        settingsUI.goTags = settings.goTags
        settingsUI.goOS = settings.goOS
        settingsUI.goArch = settings.goArch
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
        // Apply settings from ui to the model
        apply()
        val executor = infoExtractor.assembleTinyGoShellCommand()
        val processHistory = GoHistoryProcessListener()
        executor.executeWithProgress(true, true, processHistory, null) { result ->
            val output = processHistory.output.joinToString("")
            logger.trace(output)
            infoExtractor.extractTinyGoInfo(output)

            val settings = TinyGoConfiguration.getInstance(project)
            settingsUI.goArch = settings.goArch
            settingsUI.goOS = settings.goOS
            settingsUI.goTags = settings.goTags
            settingsUI.onProcessingFinished()
            propagateGoSettings()
        }
    }

    private fun propagateGoSettings() {
        val goSettings = ModuleManager.getInstance(project).modules.map {
            it?.getService(GoModuleSettings::class.java)
        }.filterNotNull().firstOrNull()
        if (goSettings == null) {
            logger.warn("Could not find go module settings")
            return
        }
        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        val buildSettings = goSettings.buildTargetSettings
        buildSettings.arch = tinyGoSettings.goArch
        buildSettings.os = tinyGoSettings.goOS
        buildSettings.customFlags = tinyGoSettings.goTags.split(' ').toTypedArray()
        goSettings.buildTargetSettings = buildSettings
    }
}
