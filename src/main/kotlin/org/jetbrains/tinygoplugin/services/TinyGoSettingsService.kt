package org.jetbrains.tinygoplugin.services

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.TinyGoSettingsUI
import java.io.File
import javax.swing.JComponent

class TinyGoSettingsService(p: Project) : Configurable {

    lateinit var settingsUI: TinyGoSettingsUI
    val project: Project = p

    private fun setSettingsToUI(settings: TinyGoConfiguration) {
        settingsUI.targetPlatform = settings.targetPlatform
        settingsUI.tinyGoPath = settings.tinyGoSDKPath.absolutePath.toString()
    }

    override fun createComponent(): JComponent {
        settingsUI = TinyGoSettingsUI()
        val settings = TinyGoConfiguration.getInstance(project)
        setSettingsToUI(settings)
        return settingsUI.mainPanel
    }

    override fun isModified(): Boolean {
        fun compareFile(file: File, str: String): Boolean = file.absolutePath == File(str).absolutePath
        val settings = TinyGoConfiguration.getInstance(project)

        return listOf(
            settingsUI.targetPlatform != settings.targetPlatform,
            !compareFile(str = settingsUI.tinyGoPath, file = settings.tinyGoSDKPath)
        ).any { it }
    }

    override fun apply() {
        val settings = TinyGoConfiguration.getInstance(project)
        settings.targetPlatform = settingsUI.targetPlatform
        settings.tinyGoSDKPath = File(settingsUI.tinyGoPath)
    }

    override fun getDisplayName(): String = "TinyGoPlugin"
}