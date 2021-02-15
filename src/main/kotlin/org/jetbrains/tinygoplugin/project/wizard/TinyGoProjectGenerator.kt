package org.jetbrains.tinygoplugin.project.wizard

import com.goide.GoConstants
import com.goide.GoIcons
import com.goide.project.GoProjectLibrariesService
import com.goide.wizard.GoProjectGenerator
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import javax.swing.Icon

class TinyGoProjectGenerator : GoProjectGenerator<TinyGoNewProjectSettings>() {
    override fun getDescription(): String = "My description"

    override fun getName(): String = "TinyGo"

    override fun getLogo(): Icon = GoIcons.ICON

    override fun validate(baseDirPath: String): ValidationResult = ValidationResult.OK

    override fun doGenerateProject(project: Project, baseDir: VirtualFile, settings: TinyGoNewProjectSettings, module: Module) {
        // TODO: set TinyGo path project-wide
    }

    override fun createPeer(): ProjectGeneratorPeer<TinyGoNewProjectSettings> {
        return TinyGoProjectGeneratorPeer()
    }

}
