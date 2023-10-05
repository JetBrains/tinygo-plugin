package org.jetbrains.tinygoplugin.runconfig

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.rd.doIfAlive
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.MappedGraphProperty
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import javax.swing.JComponent
import javax.swing.JTextField
import kotlin.reflect.KMutableProperty1

class RunConfigurationWrapper(private val configurationProvider: ConfigurationProvider<RunSettings>) :
    TinyGoPropertiesWrapper(configurationProvider) {
    inner class RunConfigurationProperty<T>(
        prop: GraphProperty<T>,
        objProperty: KMutableProperty1<RunSettings, T>,
    ) : MappedGraphProperty<T, RunSettings>(prop, objProperty, configurationProvider, this)

    val mainFile = RunConfigurationProperty(
        prop = propertyGraph.lazyProperty(configurationProvider.tinyGoSettings::mainFile),
        objProperty = RunSettings::mainFile
    )

    val cmdLineArguments = RunConfigurationProperty(
        prop = propertyGraph.lazyProperty(configurationProvider.tinyGoSettings::userArguments),
        objProperty = RunSettings::userArguments
    )

    val outputPath = RunConfigurationProperty(
        prop = propertyGraph.lazyProperty(configurationProvider.tinyGoSettings::outputPath),
        objProperty = RunSettings::outputPath
    )
}

private const val TARGET_LABEL = "ui.target"
private const val CLI_ARGUMENTS_LABEL = "ui.cli"
private const val PATH_TO_SRC_LABEL = "ui.src"
private const val ENVIRONMENT_LABEL = "ui.environment"

open class TinyGoRunConfigurationEditor<RunConfigurationType : TinyGoRunConfiguration>(
    private val runConfiguration: RunConfigurationType,
    private val pathKind: PathKind
) : SettingsEditor<RunConfigurationType>() {
    enum class PathKind(private val string: String) {
        MAIN("main.go"), TEST("test file");
        override fun toString(): String = string
    }

    // Property binding mechanism isn't used because the EnvironmentVariablesTextFieldWithBrowseButton
    // component fires its specific event, which is not captured by the mechanism
    private val environmentEditor = EnvironmentVariablesTextFieldWithBrowseButton()

    protected val properties = RunConfigurationWrapper(runConfiguration)

    init {
        resetEditorFrom(runConfiguration)
    }

    final override fun resetEditorFrom(configuration: RunConfigurationType) {
        runConfiguration.runConfig = configuration.runConfig
        environmentEditor.envs = configuration.customEnvironment
        environmentEditor.isPassParentEnvs = configuration.isPassParentEnvironment
        properties.reset()
    }

    final override fun applyEditorTo(tinyGoRunConfiguration: RunConfigurationType) {
        tinyGoRunConfiguration.runConfig = runConfiguration.runConfig.deepCopy()
        tinyGoRunConfiguration.customEnvironment = environmentEditor.envs
        tinyGoRunConfiguration.isPassParentEnvironment = environmentEditor.isPassParentEnvs
    }

    override fun createEditor(): JComponent {
        return panel {
            row(TinyGoBundle.message(TARGET_LABEL)) {
                targetPlatformFieldWithLink(this, properties, runConfiguration, this@TinyGoRunConfigurationEditor)
            }
            row(TinyGoBundle.message(CLI_ARGUMENTS_LABEL)) {
                textField()
                    .align(Align.FILL)
                    .bindText(properties.cmdLineArguments)
                    .columns(COLUMNS_MEDIUM)
            }
            row(TinyGoBundle.message(PATH_TO_SRC_LABEL, pathKind)) {
                val fileChooserDescriptor = FileChooserDescriptor(true, true, false, false, false, false)
                textFieldWithBrowseButton(fileChooserDescriptor = fileChooserDescriptor)
                    .align(Align.FILL)
                    .bindText(properties.mainFile)
            }
            row(TinyGoBundle.message(ENVIRONMENT_LABEL)) {
                cell(environmentEditor).align(Align.FILL)
            }
            createAdditionalComponent(this)
        }
    }

    protected open fun createAdditionalComponent(panel: Panel) = Unit
}

private const val OUTPUT_PATH_LABEL = "ui.output.path"

class TinyGoBuildRunConfigurationEditor(runConfiguration: TinyGoBuildRunConfiguration) :
    TinyGoRunConfigurationEditor<TinyGoBuildRunConfiguration>(runConfiguration, PathKind.MAIN) {
    override fun createAdditionalComponent(panel: Panel) {
        with(panel) {
            row(TinyGoBundle.message(OUTPUT_PATH_LABEL)) {
                val fileChooserDescriptor = FileChooserDescriptor(false, true, false, false, false, false)
                textFieldWithBrowseButton(fileChooserDescriptor = fileChooserDescriptor)
                    .align(Align.FILL)
                    .bindText(properties.outputPath)
            }
        }
    }
}

private fun targetPlatformFieldWithLink(
    row: Row,
    properties: RunConfigurationWrapper,
    runConfiguration: TinyGoRunConfiguration,
    parentDisposable: Disposable
) {
    with(row) {
        val targetTextField = JTextField(properties.target.get())
        targetTextField.isEnabled = false
        val targetFieldWithLink = TextFieldWithBrowseButton(targetTextField) {
            val project = runConfiguration.project
            val edited = service<ShowSettingsUtil>().editConfigurable(project, TinyGoSettingsService(project))
            if (edited) {
                parentDisposable.doIfAlive {
                    targetTextField.text = project.tinyGoConfiguration().targetPlatform
                }
            }
        }
        cell(targetFieldWithLink).align(Align.FILL)
    }
}
