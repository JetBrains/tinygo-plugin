package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.combobox.GoSdkChooserCombo
import com.goide.wizard.GoProjectGeneratorPeer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateTinyGoParametersPanel
import javax.swing.BoxLayout
import javax.swing.JPanel

class TinyGoProjectGeneratorPeer :
    GoProjectGeneratorPeer<TinyGoNewProjectSettings>(),
    ConfigurationProvider<TinyGoConfiguration> {
    override var tinyGoSettings: TinyGoConfiguration = ConfigurationWithHistory()

    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun createSettingsPanel(
        parentDisposable: Disposable,
        locationComponent: LabeledComponent<TextFieldWithBrowseButton>?,
        sdkCombo: GoSdkChooserCombo?,
        project: Project?,
    ): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(createGridPanel(locationComponent, sdkCombo).resize().createPanel())
        panel.add(generateTinyGoParametersPanel(project, propertiesWrapper, parentDisposable))
        return panel
    }

    override fun getSettings(): TinyGoNewProjectSettings = TinyGoNewProjectSettings(sdkFromCombo, tinyGoSettings)
}
