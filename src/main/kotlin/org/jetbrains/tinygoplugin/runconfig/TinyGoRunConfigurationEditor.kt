package org.jetbrains.tinygoplugin.runconfig

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.MappedGraphProperty
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1

class RunConfigurationWrapper(private val configurationProvider: ConfigurationProvider<RunSettings>) :
    TinyGoPropertiesWrapper(configurationProvider) {
    inner class RunConfigurationProperty<T>(
        prop: GraphProperty<T>,
        objProperty: KMutableProperty1<RunSettings, T>,
    ) : MappedGraphProperty<T, RunSettings>(prop, objProperty, configurationProvider, this)

    val mainFile = RunConfigurationProperty(
        prop = propertyGraph.graphProperty(configurationProvider.tinyGoSettings::mainFile),
        objProperty = RunSettings::mainFile
    )

    val cmdLineArguments = RunConfigurationProperty(
        prop = propertyGraph.graphProperty(configurationProvider.tinyGoSettings::userArguments),
        objProperty = RunSettings::userArguments
    )
}

class TinyGoRunConfigurationEditor(
    private val runConfiguration: TinyGoRunConfiguration,
    private val pathKind: PathKind
) : SettingsEditor<TinyGoRunConfiguration>() {
    enum class PathKind(private val string: String) {
        MAIN("main.go"), TEST("test file");
        override fun toString(): String = string
    }

    private val properties = RunConfigurationWrapper(runConfiguration)

    init {
        resetEditorFrom(runConfiguration)
    }

    override fun resetEditorFrom(configuration: TinyGoRunConfiguration) {
        runConfiguration.runConfig = configuration.runConfig
        properties.reset()
    }

    override fun applyEditorTo(tinyGoRunConfiguration: TinyGoRunConfiguration) {
        tinyGoRunConfiguration.runConfig = runConfiguration.runConfig.deepCopy()
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Target") {
                textField()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(properties.target)
                    .enabled(false)
            }
            row("Command line arguments") {
                textField()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(properties.cmdLineArguments)
                    .columns(COLUMNS_MEDIUM)
            }
            row("Path to $pathKind") {
                val fileChooserDescriptor = FileChooserDescriptor(true, true, false, false, false, false)
                textFieldWithBrowseButton(fileChooserDescriptor = fileChooserDescriptor)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindText(properties.mainFile)
            }
        }
    }
}
