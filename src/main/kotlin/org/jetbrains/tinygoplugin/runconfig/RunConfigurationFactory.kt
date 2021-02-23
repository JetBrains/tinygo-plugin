package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoConfigurationFactoryBase
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons

internal const val CONFIGURATION_NAME = "TinyGo"
internal const val DESCRIPTION = "TinyGo Flash Application"
internal const val TINYGO_CONFIGURATION_ID = "tinyGoConfigurationId"
internal const val FACTORY_ID = "tinyGoFactoryId"

class TinyGoConfigurationFactory(
    type: ConfigurationType,
    private val runType: TinyGoCommandType,
) : GoConfigurationFactoryBase(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TinyGoRunConfiguration(project, this, project.name, runType)
    }

    override fun getId(): String =
        FACTORY_ID + runType.command

    override fun getName(): String {
        return "$CONFIGURATION_NAME ${runType.command}"
    }
}

class TinyGoRunConfigurationType :
    ConfigurationTypeBase(TINYGO_CONFIGURATION_ID, CONFIGURATION_NAME, DESCRIPTION, TinyGoPluginIcons.TinyGoIcon) {
    init {
        addFactory(TinyGoConfigurationFactory(this, TinyGoRunCommand()))
        addFactory(TinyGoConfigurationFactory(this, TinyGoFlashCommand()))
    }
}
