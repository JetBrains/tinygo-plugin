package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.sdk.nullSdk

interface TinyGoConfiguration : UserConfiguration, ProjectConfiguration {
    fun deepCopy(): TinyGoConfiguration
    fun saveState(project: Project)
    fun modified(project: Project): Boolean
    val enabled: Boolean

    companion object {
        fun getInstance(p: Project): TinyGoConfiguration = TinyGoConfigurationImpl.getInstance(p)

        fun getInstance(): TinyGoConfiguration = TinyGoConfigurationImpl()
    }
}

internal data class TinyGoConfigurationImpl(
    private val userConfig: UserConfigurationStorageWrapper = UserConfigurationStorageWrapper(),
    private val projectConfig: ProjectConfigurationState = ProjectConfigurationState(),
) : TinyGoConfiguration, UserConfiguration by userConfig, ProjectConfiguration by projectConfig {

    override fun saveState(project: Project) {
        project.service<ProjectConfigurationImpl>().myState = projectConfig.copy()
        project.service<UserConfigurationImpl>().myState = userConfig.copy()
    }

    override fun modified(project: Project): Boolean {
        val currentSettings = getInstance(project)
        return currentSettings.projectConfig != projectConfig ||
                currentSettings.userConfig != userConfig
    }

    override fun deepCopy(): TinyGoConfigurationImpl {
        val projectConfigurationCopy = projectConfig.copy()
        val userConfigurationCopy = userConfig.copy()
        return TinyGoConfigurationImpl(
            projectConfig = projectConfigurationCopy,
            userConfig = userConfigurationCopy,
        )
    }

    override val enabled: Boolean
        get() = sdk != nullSdk

    companion object {
        fun getInstance(p: Project): TinyGoConfigurationImpl = TinyGoConfigurationImpl(
            projectConfig = p.service<ProjectConfigurationImpl>().myState,
            userConfig = p.service<UserConfigurationImpl>().myState,
        )
    }
}
