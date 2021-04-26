package org.jetbrains.tinygoplugin.project.wizard

import com.goide.GoIcons
import com.goide.vgo.wizard.VgoModuleBuilder
import com.goide.vgo.wizard.VgoNewProjectSettings
import com.goide.wizard.GoProjectGenerator
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkVersion
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.services.propagateGoFlags
import javax.swing.Icon

@Suppress("MagicNumber")
val tinyGoVersionWithModules = TinyGoSdkVersion(0, 14, 0)

class TinyGoProjectGenerator : GoProjectGenerator<TinyGoNewProjectSettings>() {
    override fun getDescription(): String = "TinyGo project"

    override fun getName(): String = "TinyGo"

    override fun getLogo(): Icon = GoIcons.ICON

    override fun validate(baseDirPath: String): ValidationResult = ValidationResult.OK

    private fun extractTinyGoSettings(project: Project, tinyGoSettings: TinyGoConfiguration) {
        TinyGoInfoExtractor(project).extractTinyGoInfo(tinyGoSettings) { _, output ->
            tinyGoSettings.extractTinyGoInfo(output)
            tinyGoSettings.saveState(project)
            propagateGoFlags(project, tinyGoSettings)
        }
    }

    override fun doGenerateProject(
        project: Project,
        baseDir: VirtualFile,
        newProjectSettings: TinyGoNewProjectSettings,
        module: Module,
    ) {
        newProjectSettings.tinyGoSettings.saveState(project)
        extractTinyGoSettings(project, newProjectSettings.tinyGoSettings)
        if (newProjectSettings.tinyGoSettings.sdk.sdkVersion.isAtLeast(tinyGoVersionWithModules)) {
            VgoModuleBuilder.vgoModuleCreated(
                module,
                VgoNewProjectSettings(newProjectSettings.goSdk, emptyMap(), true),
                true,
                baseDir.path
            )
        }
    }

    override fun createPeer(): ProjectGeneratorPeer<TinyGoNewProjectSettings> = TinyGoProjectGeneratorPeer()
}
