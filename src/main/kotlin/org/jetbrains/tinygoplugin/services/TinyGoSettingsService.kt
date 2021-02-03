package org.jetbrains.tinygoplugin.services

import com.intellij.openapi.options.Configurable
import org.jetbrains.tinygoplugin.TinyGoPluginConfiguration
import org.jetbrains.tinygoplugin.ui.TinyGoSettingsUI
import java.io.File
import javax.swing.JComponent

class TinyGoSettingsService : Configurable {

    lateinit var settingsUI: TinyGoSettingsUI

    private fun setSettingsToUI(settings: TinyGoPluginConfiguration) {
        settingsUI.targetPlatform = settings.targetPlatform
        settingsUI.tinyGoGOPATH = settings.gopath.absolutePath.toString()
        settingsUI.tinyGoPath = settings.tinyGoExecutablePath.absolutePath.toString()
    }

    override fun createComponent(): JComponent {
        settingsUI = TinyGoSettingsUI()
        val settings = TinyGoPluginConfiguration.instance
        setSettingsToUI(settings)
        return settingsUI.mainPanel
    }

    override fun isModified(): Boolean {
        fun compareFile(file: File, str: String): Boolean = file.absolutePath == File(str).absolutePath
        val settings = TinyGoPluginConfiguration.instance

        return listOf(
            settingsUI.targetPlatform != settings.targetPlatform,
            !compareFile(str = settingsUI.tinyGoGOPATH, file = settings.gopath),
            !compareFile(str = settingsUI.tinyGoPath, file = settings.tinyGoExecutablePath)
        ).any { it }
    }

    override fun apply() {
        val settings = TinyGoPluginConfiguration.instance
        settings.targetPlatform = settingsUI.targetPlatform
        settings.gopath = File(settingsUI.tinyGoGOPATH)
        settings.tinyGoExecutablePath = File(settingsUI.tinyGoPath)
    }

    override fun getDisplayName(): String = "TinyGoPlugin"
}