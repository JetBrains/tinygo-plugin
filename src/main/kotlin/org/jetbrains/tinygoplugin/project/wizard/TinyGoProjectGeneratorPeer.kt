package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.combobox.GoSdkChooserCombo
import com.goide.wizard.GoProjectGeneratorPeer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.UI.PanelFactory
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.checkDirectoryForTinyGo
import org.jetbrains.tinygoplugin.sdk.suggestSdkDirectoryStr
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateTinyGoParametersPanel
import javax.swing.BoxLayout
import javax.swing.JPanel

class TinyGoProjectGeneratorPeer :
    GoProjectGeneratorPeer<TinyGoNewProjectSettings>(),
    ConfigurationProvider<TinyGoConfiguration> {
    override var tinyGoSettings: TinyGoConfiguration = TinyGoConfiguration.getInstance()

    init {
        tinyGoSettings.tinyGoSDKPath = suggestSdkDirectoryStr()
    }

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
        panel.add(
            decorateSettingsPanelForUI(
                generateTinyGoParametersPanel(
                    propertiesWrapper,
                    {
                        if (checkDirectoryForTinyGo(it)) it.canonicalPath!!
                        else {
                            Messages.showErrorDialog("Selected TinyGo path is invalid", "Invalid TinyGo")
                            suggestSdkDirectoryStr()
                        }
                    },
                )
            ),
        )
        return panel
    }

    override fun getSettings(): TinyGoNewProjectSettings = TinyGoNewProjectSettings(sdkFromCombo, tinyGoSettings)
}
