package org.jetbrains.tinygoplugin

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.configuration.ProjectConfiguration
import org.jetbrains.tinygoplugin.configuration.ProjectConfigurationImpl
import org.jetbrains.tinygoplugin.configuration.ProjectConfigurationState
import org.jetbrains.tinygoplugin.configuration.UserConfiguration
import org.jetbrains.tinygoplugin.configuration.UserConfigurationImpl
import org.jetbrains.tinygoplugin.configuration.UserConfigurationState

data class TinyGoConfiguration(
    private val userConfig: UserConfigurationState = UserConfigurationState(),
    private val projectConfig: ProjectConfigurationState = ProjectConfigurationState(),
    private val project: Project,
) :
    UserConfiguration by userConfig, ProjectConfiguration by projectConfig {

    fun saveState(p: Project) {
        p.service<ProjectConfigurationImpl>().projectState = projectConfig.copy()
        p.service<UserConfigurationImpl>().userConfigurationState = userConfig.copy()
    }

    fun modified(p: Project): Boolean {
        val currentSettings = getInstance(p)
        return currentSettings.projectConfig != projectConfig ||
            currentSettings.userConfig != userConfig
    }

    fun deepCopy(): TinyGoConfiguration {
        val projectConfigurationCopy = projectConfig.copy()
        val userConfigurationCopy = userConfig.copy()
        return TinyGoConfiguration(
            projectConfig = projectConfigurationCopy,
            userConfig = userConfigurationCopy,
            project = project
        )
    }

    companion object {
        fun getInstance(p: Project): TinyGoConfiguration = TinyGoConfiguration(
            projectConfig = p.service<ProjectConfigurationImpl>().state,
            userConfig = p.service<UserConfigurationImpl>().state,
            project = p
        )
    }
}
