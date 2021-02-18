package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

data class TinyGoConfiguration(
    private val project: Project?,
    private val userConfig: UserConfigurationState = UserConfigurationState(),
    private val projectConfig: ProjectConfigurationState = ProjectConfigurationState(),
) : UserConfiguration by userConfig, ProjectConfiguration by projectConfig {

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
