package org.jetbrains.tinygoplugin.project.wizard

import com.goide.project.GoModuleBuilderBase
import com.goide.project.GoProjectLifecycle
import com.goide.vgo.wizard.VgoModuleBuilder
import com.goide.vgo.wizard.VgoNewProjectSettings
import com.goide.wizard.GoProjectGenerator
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import kotlinx.coroutines.launch
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.sendReloadLibrariesSignal
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkVersion
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.TinyGoServiceScope
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.services.propagateGoFlags
import javax.swing.Icon

@Suppress("MagicNumber")
val tinyGoVersionWithModules = TinyGoSdkVersion(0, 14, 0)

private fun getProjectPresentableName() = TinyGoBundle.message("name")
private fun getProjectDescription(): String = TinyGoBundle.message("new.project.description")
private fun getProjectIcon(): Icon = TinyGoPluginIcons.TinyGoIcon

class TinyGoProjectGenerator : GoProjectGenerator<TinyGoNewProjectSettings>() {
    override fun getName(): String = getProjectPresentableName()

    override fun getDescription(): String = getProjectDescription()

    override fun getLogo(): Icon = getProjectIcon()

    override fun validate(baseDirPath: String): ValidationResult = ValidationResult.OK

    override fun doGenerateProject(
        project: Project,
        baseDir: VirtualFile,
        newProjectSettings: TinyGoNewProjectSettings,
        module: Module,
    ) {
        thisLogger().debug("Begin configuring module for a new project")
        configureModule(newProjectSettings, project, module, baseDir.path)
        GoProjectLifecycle.runWhenProjectSetupFinished(project) {
            sendReloadLibrariesSignal(project)
        }
        thisLogger().debug("Finish configuring module for a new project")
    }

    override fun createPeer(): ProjectGeneratorPeer<TinyGoNewProjectSettings> = TinyGoProjectGeneratorPeer()
}

class TinyGoModuleBuilder : GoModuleBuilderBase<TinyGoNewProjectSettings>(TinyGoProjectGeneratorPeer()) {
    override fun getPresentableName(): String = getProjectPresentableName()

    override fun getDescription(): String = getProjectDescription()

    override fun getNodeIcon(): Icon = getProjectIcon()

    override fun moduleCreated(module: Module, isCreatingNewProject: Boolean) {
        thisLogger().debug("Begin configuring module for module creation")
        configureModule(settings, module.project, module, contentEntryPath)
        thisLogger().debug("Finish configuring module for module creation")
    }
}

private fun configureModule(
    newProjectSettings: TinyGoNewProjectSettings,
    project: Project,
    module: Module,
    contentRoot: String?
) {
    newProjectSettings.tinyGoSettings.saveState(project)
    extractTinyGoSettings(project, newProjectSettings.tinyGoSettings)
    if (newProjectSettings.tinyGoSettings.sdk.sdkVersion.isAtLeast(tinyGoVersionWithModules)) {
        VgoModuleBuilder.vgoModuleCreated(
            module,
            VgoNewProjectSettings(newProjectSettings.goSdk, emptyMap(), true, false),
            true,
            contentRoot
        )
    }
}

private fun extractTinyGoSettings(project: Project, tinyGoSettings: TinyGoConfiguration) {
    TinyGoServiceScope.getScope(project).launch {
        project.service<TinyGoInfoExtractor>().extractTinyGoInfo(tinyGoSettings) { _, output ->
            TinyGoServiceScope.getScope(project).launch {
                tinyGoSettings.extractTinyGoInfo(output)
                writeAction {
                    tinyGoSettings.saveState(project)
                    propagateGoFlags(project, tinyGoSettings)
                }
            }
        }
    }
}
