package org.jetbrains.tinygoplugin.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import javax.swing.Icon

internal const val CONFIGURATION_ID = "TinyGO"
internal const val CONFIGURATION_NAME = "TinyGO Flash"
internal const val DESCRIPTION = "TinyGo Flash Application"
internal const val TINYGO_CONFIGURATION_ID = "TinyGoId"
internal const val FACTORY_ID = "TinyGo Application"

class TinyGoConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TinyGoFlashConfiguration(project, this, project.name)
    }

    override fun getName(): String {
        return FACTORY_NAME
    }

    override fun getId(): String =
        FACTORY_ID

    companion object {
        private const val FACTORY_NAME = "Demo configuration factory"
    }
}

class TinyGoRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = CONFIGURATION_NAME

    override fun getConfigurationTypeDescription(): String = DESCRIPTION

    override fun getIcon(): Icon {
        return AllIcons.General.Information
    }

    override fun getId(): String = TINYGO_CONFIGURATION_ID

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf<ConfigurationFactory>(TinyGoConfigurationFactory(this))
    }
}
