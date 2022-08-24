package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.combobox.GoSdkChooserCombo
import com.goide.wizard.GoProjectGeneratorPeer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.UI.PanelFactory
import com.intellij.util.ui.UIUtil
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateTinyGoParametersPanel
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class TinyGoProjectGeneratorPeer :
    GoProjectGeneratorPeer<TinyGoNewProjectSettings>(),
    ConfigurationProvider<TinyGoConfiguration> {
    override var tinyGoSettings: TinyGoConfiguration = ConfigurationWithHistory()

    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    private fun decorateSettingsPanelForUI(component: JPanel): JPanel =
        PanelFactory.grid().add(PanelFactory.panel(component)).resize().createPanel()

    override fun createSettingsPanel(
        parentDisposable: Disposable,
        locationComponent: LabeledComponent<TextFieldWithBrowseButton>?,
        sdkCombo: GoSdkChooserCombo?,
        project: Project?,
    ): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(createGridPanel(locationComponent, sdkCombo).resize().createPanel())
        val projectPathSupplier = locationComponent!!.component::getText
        panel.add(
            decorateSettingsPanelForUI(
                generateTinyGoParametersPanel(projectPathSupplier, propertiesWrapper, parentDisposable)
            ).apply {
                // TinyGo settings panel doesn't have borders
                // like regular Go settings fields do, so they are inherited here
                val defaultTextBoxBorder = UIUtil.getTextFieldBorder().getBorderInsets(sdkCombo)
                border = EmptyBorder(defaultTextBoxBorder.apply { left = 0 })
            }
        )
        return panel
    }

    override fun getSettings(): TinyGoNewProjectSettings = TinyGoNewProjectSettings(sdkFromCombo, tinyGoSettings)
}
