package org.jetbrains.tinygoplugin.services

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import javax.swing.Icon
import javax.swing.JComponent

internal const val CONFIGURATION_ID = "TinyGO"
internal const val CONFIGURATION_NAME = "TinyGO Flash"
internal const val DESCRIPTION = "TinyGo Flash Application"
internal const val FACTORY_ID = "TinyGo Application"

class TinyGoConfigurationEditor(private val project: Project, defaultConfiguration: DemoRunConfiguration) :
    SettingsEditor<DemoRunConfiguration>() {

    private val propertyGraph = PropertyGraph()
    private val sdkProperty = propertyGraph.graphProperty { "" }
    private val gcProperty = propertyGraph.graphProperty { GarbageCollector.AUTO_DETECT }
    private val schedulerProperty = propertyGraph.graphProperty { Scheduler.AUTO_DETECT }
    private val targetProperty = propertyGraph.graphProperty { "" }
    private val tinyGoArguments = propertyGraph.graphProperty { "" }

    init {
        resetEditorFrom(defaultConfiguration)
    }

    override fun resetEditorFrom(configuration: DemoRunConfiguration) {
        sdkProperty.set(configuration.tinyGoSDKPath)
        sdkProperty.set(configuration.tinyGoSDKPath)
        gcProperty.set(configuration.gc)
        schedulerProperty.set(configuration.scheduler)
        targetProperty.set(configuration.target)

        /* ktlint-disable */
        tinyGoArguments.set(
            listOf(
                "flash",
                "-target", configuration.target,
                "-scheduler", configuration.scheduler.cmd,
                "-gc", configuration.gc.cmd
            ).joinToString(separator = " ")
            /* ktlint-enable */
        )
    }

    override fun applyEditorTo(demoRunConfiguration: DemoRunConfiguration) {
        demoRunConfiguration.cmdlineOptions = tinyGoArguments.get().split(' ').filter {
            it.trim().isNotEmpty()
        }.toMutableList()
    }

    override fun createEditor(): JComponent {
        return panel {
            row("TinyGo path") {
                textField(sdkProperty).enabled(false)
            }
            row("Target") {
                textField(targetProperty).enabled(false)
            }
            row("Command line arguments") {
                textField(tinyGoArguments).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
        }
    }
}

class DemoRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<Any?>(project, factory, name) {
    var tinyGoSDKPath: String = ""
    var target = ""
    var gc = GarbageCollector.AUTO_DETECT
    var scheduler = Scheduler.AUTO_DETECT
    var cmdlineOptions: MutableCollection<String> = ArrayList()

    init {
        val settings = TinyGoConfiguration.getInstance(project)
        tinyGoSDKPath = settings.tinyGoSDKPath
        target = settings.targetPlatform
        gc = settings.gc
        scheduler = settings.scheduler
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return null
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return TinyGoConfigurationEditor(project, this)
    }
    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
    }
}

class DemoConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return DemoRunConfiguration(project, this, "Demo")
    }

    override fun getName(): String {
        return FACTORY_NAME
    }

    override fun getId(): String =
        CONFIGURATION_ID

    companion object {
        private const val FACTORY_NAME = "Demo configuration factory"
    }
}

class DemoRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String {
        return "TinyGoDisplayName"
    }

    override fun getConfigurationTypeDescription(): String {
        return "TinyGoConfigurationTypeDescriptor"
    }

    override fun getIcon(): Icon {
        return AllIcons.General.Information
    }

    override fun getId(): String {
        return "TinyGoId"
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf<ConfigurationFactory>(DemoConfigurationFactory(this))
    }
}
