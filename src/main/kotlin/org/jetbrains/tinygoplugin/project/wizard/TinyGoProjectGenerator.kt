package org.jetbrains.tinygoplugin.project.wizard

import com.goide.GoIcons
import com.goide.wizard.GoProjectGenerator
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import javax.swing.Icon

class TinyGoProjectGenerator : GoProjectGenerator<TinyGoNewProjectSettings>() {
    override fun getDescription(): String = "TinyGo project"

    override fun getName(): String = "TinyGo"

    override fun getLogo(): Icon = GoIcons.ICON

    override fun validate(baseDirPath: String): ValidationResult = ValidationResult.OK

    override fun doGenerateProject(
        project: Project,
        baseDir: VirtualFile,
        newProjectSettings: TinyGoNewProjectSettings,
        module: Module
    ) {
        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        tinyGoSettings.tinyGoSDKPath = newProjectSettings.tinyGoSdkPath
        tinyGoSettings.targetPlatform = newProjectSettings.tinyGoTarget
        tinyGoSettings.saveState(project)
    }

    override fun createPeer(): ProjectGeneratorPeer<TinyGoNewProjectSettings> {
        return TinyGoProjectGeneratorPeer()
    }
}
