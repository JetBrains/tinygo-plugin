package org.jetbrains.tinygoplugin.project.wizard

import com.goide.GoIcons
import com.goide.util.GoHistoryProcessListener
import com.goide.wizard.GoProjectGenerator
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkUtil
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import javax.swing.Icon

class TinyGoProjectGenerator : GoProjectGenerator<TinyGoNewProjectSettings>() {
    override fun getDescription(): String = "TinyGo project"

    override fun getName(): String = "TinyGo"

    override fun getLogo(): Icon = GoIcons.ICON

    override fun validate(baseDirPath: String): ValidationResult = ValidationResult.OK

    private fun extractTinyGoSettings(project: Project, tinyGoSettings: TinyGoConfiguration) {
        val processHistory = GoHistoryProcessListener()
        TinyGoInfoExtractor(project).extractTinyGoInfo(tinyGoSettings, processHistory) {
            val output = processHistory.output.joinToString("")
            tinyGoSettings.extractTinyGoInfo(output)
            tinyGoSettings.saveState(project)
        }
    }

    override fun doGenerateProject(
        project: Project,
        baseDir: VirtualFile,
        newProjectSettings: TinyGoNewProjectSettings,
        module: Module
    ) {
        newProjectSettings.tinyGoSettings.saveState(project)
        extractTinyGoSettings(project, newProjectSettings.tinyGoSettings)

        TinyGoSdkUtil.notifyTinyGoNotConfigured(
            project,
            "Please set TinyGo fully up to complete project building, running and indexing"
        )
    }

    override fun createPeer(): ProjectGeneratorPeer<TinyGoNewProjectSettings> {
        return TinyGoProjectGeneratorPeer()
    }
}
