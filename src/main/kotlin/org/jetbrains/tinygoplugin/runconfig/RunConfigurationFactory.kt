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

open class TinyGoConfigurationFactory(
    type: ConfigurationType,
    private val runType: TinyGoCommandType,
) : GoConfigurationFactoryBase(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TinyGoRunConfigurationImpl(project, this, project.name, runType)
    }

    override fun getId(): String = FACTORY_ID + runType.command

    override fun getName(): String = "$CONFIGURATION_NAME ${runType.command}"
}

class TinyGoTestConfigurationFactory(type: ConfigurationType) : TinyGoConfigurationFactory(type, TinyGoTestCommand) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TinyGoTestRunConfiguration(project, this, project.name)
    }
}

class TinyGoBuildConfigurationFactory(type: ConfigurationType) : TinyGoConfigurationFactory(type, TinyGoBuildCommand) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TinyGoBuildRunConfiguration(project, this, project.name)
    }
}

class TinyGoHeapAllocConfigurationFactory(type: ConfigurationType) :
    TinyGoConfigurationFactory(type, TinyGoBuildCommand) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TinyGoHeapAllocRunConfiguration(project, this, project.name)
    }

    override fun getId(): String = FACTORY_ID + "heapalloc"

    override fun getName(): String = "$CONFIGURATION_NAME heapalloc"
}

class TinyGoRunConfigurationType :
    ConfigurationTypeBase(TINYGO_CONFIGURATION_ID, CONFIGURATION_NAME, DESCRIPTION, TinyGoPluginIcons.TinyGoIcon) {
    val runFactory = TinyGoConfigurationFactory(this, TinyGoRunCommand)
    val flashFactory = TinyGoConfigurationFactory(this, TinyGoFlashCommand)
    val testFactory = TinyGoTestConfigurationFactory(this)
    val buildFactory = TinyGoBuildConfigurationFactory(this)
    val heapAllocFactory = TinyGoHeapAllocConfigurationFactory(this)

    init {
        addFactory(runFactory)
        addFactory(flashFactory)
        addFactory(testFactory)
        addFactory(buildFactory)
        addFactory(heapAllocFactory)
    }
}
