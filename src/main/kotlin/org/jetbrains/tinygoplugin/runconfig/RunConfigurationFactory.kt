package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoConfigurationFactoryBase
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons

internal const val CONFIGURATION_ID = "TinyGO"
internal const val CONFIGURATION_NAME = "Tiny GO"
internal const val DESCRIPTION = "TinyGo Flash Application"
internal const val TINYGO_CONFIGURATION_ID = "TinyGoId"
internal const val FACTORY_ID = "TinyGo Application"

class TinyGoConfigurationFactory(
    type: ConfigurationType,
    private val runType: org.jetbrains.tinygoplugin.runconfig.ConfigurationType,
) : GoConfigurationFactoryBase(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TinyGoFlashConfiguration(project, this, project.name, runType)
    }

    override fun getId(): String =
        FACTORY_ID + runType.command

    override fun getName(): String {
        return "Tiny Go " + runType.command
    }

    companion object {
        private const val FACTORY_NAME = "Demo configuration factory"
    }
}

class TinyGoRunConfigurationType :
    ConfigurationTypeBase(TINYGO_CONFIGURATION_ID, CONFIGURATION_NAME, DESCRIPTION, TinyGoPluginIcons.TinyGoIcon) {
    init {
        addFactory(TinyGoConfigurationFactory(this, RunConfiguration()))
        addFactory(TinyGoConfigurationFactory(this, TestConfiguration()))
        addFactory(TinyGoConfigurationFactory(this, FlashConfiguration()))
    }
}
