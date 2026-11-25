package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.combobox.GoSdkChooserCombo
import com.goide.wizard.GoProjectGeneratorPeer
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateTinyGoNewProjectSettingsPanel
import javax.swing.BoxLayout
import javax.swing.JPanel

class TinyGoProjectGeneratorPeer :
    GoProjectGeneratorPeer<TinyGoNewProjectSettings>(),
    ConfigurationProvider<TinyGoConfiguration> {
    override var tinyGoSettings: TinyGoConfiguration =
        ConfigurationWithHistory(maintainPredefinedTargets = true)

    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun createSettingsPanel(
        parentDisposable: Disposable,
        locationComponent: LabeledComponent<TextFieldWithBrowseButton>?,
        sdkCombo: GoSdkChooserCombo?,
        project: Project?,
    ): JPanel {
        val root = JPanel()
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        root.add(
            panel {
                if (locationComponent != null) {
                    row(getLocationComponentLabelText(locationComponent)) {
                        cell(locationComponent.component).align(AlignX.FILL)
                    }
                }
                if (sdkCombo != null) {
                    row(SDK_LABEL_TEXT) {
                        cell(sdkCombo).align(AlignX.FILL)
                    }
                }
            },
        )
        root.add(
            generateTinyGoNewProjectSettingsPanel(
                project,
                { locationComponent?.text.orEmpty() },
                propertiesWrapper,
                parentDisposable
            )
        )
        return root
    }

    override fun getSettings(): TinyGoNewProjectSettings = TinyGoNewProjectSettings(sdkFromCombo, tinyGoSettings)
}
