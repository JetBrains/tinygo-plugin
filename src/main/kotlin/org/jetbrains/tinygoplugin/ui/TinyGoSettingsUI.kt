package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class TinyGoSettingsUI {
    var mainPanel: JPanel
    private var tinyGoPathText: TextFieldWithBrowseButton
    private var gopathText: TextFieldWithBrowseButton
    private var targetPlatformText: JBTextField

    init {
        tinyGoPathText = TextFieldWithBrowseButton()
        gopathText = TextFieldWithBrowseButton()
        val fileDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        @Suppress("DialogTitleCapitalization")
        gopathText.addBrowseFolderListener("Select GOPATH folder", null, null, fileDescriptor)
        @Suppress("DialogTitleCapitalization")
        tinyGoPathText.addBrowseFolderListener("Select TinyGo folder", null, null, fileDescriptor)
        gopathText.focusTraversalKeysEnabled = true
        targetPlatformText = JBTextField()
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Path to TinyGo executable: "),
                tinyGoPathText, 2, true
            )
            .addLabeledComponent(
                JBLabel("Path to TinyGo GOPATH: "),
                gopathText, 1, true
            )
            .addLabeledComponent(
                JBLabel("Target platform: "),
                targetPlatformText, 1, true
            )
            .panel

    }

    var tinyGoPath: String by tinyGoPathText::text
    var tinyGoGOPATH: String by gopathText::text
    var targetPlatform: String by targetPlatformText::text

}