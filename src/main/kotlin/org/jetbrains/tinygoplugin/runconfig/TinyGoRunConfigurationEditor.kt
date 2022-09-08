package org.jetbrains.tinygoplugin.runconfig

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.rd.doIfAlive
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.services.editTinyGoSettingsLater
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
}

private const val TARGET_LABEL = "ui.target"
private const val CLI_ARGUMENTS_LABEL = "ui.cli"
private const val PATH_TO_SRC_LABEL = "ui.src"
private const val ENVIRONMENT_LABEL = "ui.environment"

class TinyGoRunConfigurationEditor(
    private val runConfiguration: TinyGoRunConfiguration,
    private val pathKind: PathKind
) : SettingsEditor<TinyGoRunConfiguration>() {
    enum class PathKind(private val string: String) {
        MAIN("main.go"), TEST("test file");
        override fun toString(): String = string
    }

    // Property binding mechanism isn't used because the EnvironmentVariablesTextFieldWithBrowseButton
    // component fires its specific event, which is not captured by the mechanism
    private val environmentEditor = EnvironmentVariablesTextFieldWithBrowseButton()

    private val properties = RunConfigurationWrapper(runConfiguration)

    init {
        resetEditorFrom(runConfiguration)
    }

    override fun resetEditorFrom(configuration: TinyGoRunConfiguration) {
        runConfiguration.runConfig = configuration.runConfig
        environmentEditor.envs = configuration.customEnvironment
        environmentEditor.isPassParentEnvs = configuration.isPassParentEnvironment
        properties.reset()
    }

    override fun applyEditorTo(tinyGoRunConfiguration: TinyGoRunConfiguration) {
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
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(properties.cmdLineArguments)
                    .columns(COLUMNS_MEDIUM)
            }
            row(TinyGoBundle.message(PATH_TO_SRC_LABEL, pathKind)) {
                val fileChooserDescriptor = FileChooserDescriptor(true, true, false, false, false, false)
                textFieldWithBrowseButton(fileChooserDescriptor = fileChooserDescriptor)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(properties.mainFile)
            }
            row(TinyGoBundle.message(ENVIRONMENT_LABEL)) {
                cell(environmentEditor).horizontalAlign(HorizontalAlign.FILL)
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
            editTinyGoSettingsLater(project) {
                parentDisposable.doIfAlive {
                    targetTextField.text = project.tinyGoConfiguration().targetPlatform
                }
            }
        }
        cell(targetFieldWithLink).horizontalAlign(HorizontalAlign.FILL)
    }
}
