package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.combobox.GoSdkChooserCombo
import com.goide.wizard.GoProjectGeneratorPeer
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkUtil
import javax.swing.JPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.util.ui.UI.PanelFactory

class TinyGoProjectGeneratorPeer : GoProjectGeneratorPeer<TinyGoNewProjectSettings>() {
    private val tinyGoSdkBrowser = textFieldWithBrowseButton(
        null, "Choose TinyGo SDK Home",
        FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        fileChosen = {
            if (TinyGoSdkUtil.checkDirectoryForTinyGo(it)) it.canonicalPath!!
            else {
                Messages.showErrorDialog("Selected TinyGo path is invalid", "Invalid TinyGo")
                TinyGoSdkUtil.suggestSdkDirectoryStr()
            }
        }
    )
    private val tinyGoTargetChooser = JBTextField()

    init {
        tinyGoSdkBrowser.text = TinyGoSdkUtil.suggestSdkDirectoryStr()
    }

    override fun createSettingsPanel(
        parentDisposable: Disposable,
        locationComponent: LabeledComponent<TextFieldWithBrowseButton>?,
        sdkCombo: GoSdkChooserCombo?,
        project: Project?
    ): JPanel = createGridPanel(locationComponent, sdkCombo)
        .add(PanelFactory.panel(tinyGoSdkBrowser).withLabel("TinyGo path:"))
        .add(PanelFactory.panel(tinyGoTargetChooser).withLabel("Target board:"))
        .resize().createPanel()

    override fun getSettings(): TinyGoNewProjectSettings {
        return TinyGoNewProjectSettings(sdkFromCombo, tinyGoSdkBrowser.text, tinyGoTargetChooser.text)
    }
}
