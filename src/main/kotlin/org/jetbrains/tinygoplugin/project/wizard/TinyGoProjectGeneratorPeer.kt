package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.combobox.GoSdkChooserCombo
import com.goide.wizard.GoProjectGeneratorPeer
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkUtil
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService
import javax.swing.JPanel
import javax.swing.JTextField

class TinyGoProjectGeneratorPeer : GoProjectGeneratorPeer<TinyGoNewProjectSettings>() {
    private var tinyGoPathBrowser: TextFieldWithBrowseButton = TextFieldWithBrowseButton()

    init {
        tinyGoPathBrowser.text = TinyGoSdkUtil.suggestSdkDirectory()?.path.toString()

        tinyGoPathBrowser.addBrowseFolderListener(
            "Select TinyGo Path", null, null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }

    override fun createSettingsPanel(
        parentDisposable: Disposable,
        locationComponent: LabeledComponent<TextFieldWithBrowseButton>?,
        sdkCombo: GoSdkChooserCombo?,
        project: Project?
    ) : JPanel {
        val locationAndCombo = createGridPanel(locationComponent, sdkCombo).resize().createPanel()
        return panel {
            row { locationAndCombo() }
            row("TinyGo Path:") { tinyGoPathBrowser() }
        }
    }

    override fun getSettings(): TinyGoNewProjectSettings {
        return TinyGoNewProjectSettings(sdkFromCombo, tinyGoPathBrowser.text)
    }

}