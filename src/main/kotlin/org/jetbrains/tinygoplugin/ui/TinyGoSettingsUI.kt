package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.jetbrains.tinygoplugin.GarbageCollector
import org.jetbrains.tinygoplugin.Scheduler
import javax.swing.JPanel


class TinyGoSettingsUI {
    var mainPanel: JPanel
    private var tinyGoPathText: TextFieldWithBrowseButton
    private var targetPlatformText: JBTextField
    companion object{
        const val gcMessage = "Garbage collector: "
        const val schedulerMessage = "Scheduler: "
    }

    private val gcComboBox: ComboBox<*>
    private val schedulerComboBox: ComboBox<*>

    init {
        tinyGoPathText = TextFieldWithBrowseButton()
        val fileDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        val gcModel = EnumComboBoxModel(GarbageCollector::class.java)
        gcComboBox = ComboBox(gcModel)

        val schedulerModel = EnumComboBoxModel(Scheduler::class.java)
        schedulerComboBox = ComboBox(schedulerModel)


        @Suppress("DialogTitleCapitalization")
        tinyGoPathText.addBrowseFolderListener("Select TinyGo folder", null, null, fileDescriptor)
        targetPlatformText = JBTextField()
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Path to TinyGo executable: "),
                tinyGoPathText, 2, true
            )
            .addLabeledComponent(
                JBLabel("Target platform: "),
                targetPlatformText, 1, true
            )
            .addLabeledComponent(
                JBLabel(gcMessage),
                gcComboBox, 1, false
            )
            .addLabeledComponent(
                JBLabel(schedulerMessage),
                schedulerComboBox, 1, false
            )
            .panel

    }

    var tinyGoPath: String by tinyGoPathText::text
    var targetPlatform: String by targetPlatformText::text

}