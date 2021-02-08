package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.jetbrains.tinygoplugin.GarbageCollector
import org.jetbrains.tinygoplugin.Scheduler
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JPanel

class TinyGoSettingsUI(buttonAction: ActionListener) {
    var mainPanel: JPanel
    private var tinyGoPathText: TextFieldWithBrowseButton
    private var targetPlatformText: JBTextField
    private var goTagsOutput: JBTextField
    private var goArchOutput: JBTextField
    private var goOSOutput: JBTextField

    companion object {
        const val gcMessage = "Garbage collector: "
        const val schedulerMessage = "Scheduler: "
    }

    private val gcComboBox: ComboBox<GarbageCollector>
    private val schedulerComboBox: ComboBox<Scheduler>
    private val detectSettings: JButton

    init {
        tinyGoPathText = TextFieldWithBrowseButton()

        goTagsOutput = JBTextField()
        goTagsOutput.isEnabled = false
        goArchOutput = JBTextField()
        goArchOutput.isEnabled = false
        goOSOutput = JBTextField()
        goOSOutput.isEnabled = false

        detectSettings = JButton("Detect")
        detectSettings.addActionListener(buttonAction)

        val fileDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        gcComboBox = ComboBox(EnumComboBoxModel(GarbageCollector::class.java))
        schedulerComboBox = ComboBox(EnumComboBoxModel(Scheduler::class.java))

        @Suppress("DialogTitleCapitalization")
        tinyGoPathText.addBrowseFolderListener("Select TinyGo folder", null, null, fileDescriptor)
        targetPlatformText = JBTextField()
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Path to TinyGo executable: "), tinyGoPathText, 2, true)
            .addLabeledComponent(JBLabel("Target platform: "), targetPlatformText, 1, true)
            .addLabeledComponent(JBLabel(gcMessage), gcComboBox, 1, false)
            .addLabeledComponent(JBLabel(schedulerMessage), schedulerComboBox, 1, false)
            .addComponent(detectSettings)
            .addLabeledComponent(JBLabel("Build tags"), goTagsOutput, 1, false)
            .addLabeledComponent(JBLabel("GOARCH"), goArchOutput, 1, false)
            .addLabeledComponent(JBLabel("GOOS"), goOSOutput, 1, false)
            .panel
    }

    var tinyGoPath: String by tinyGoPathText::text
    var targetPlatform: String by targetPlatformText::text
    var garbageCollector: GarbageCollector by gcComboBox::item
    var scheduler: Scheduler by schedulerComboBox::item
    var goTags: String by goTagsOutput::text
    var goArch: String by goArchOutput::text
    var goOS: String by goOSOutput::text

    fun setDetectionInProgress() {
        detectSettings.isEnabled = false
    }

    fun onProcessingFinished() {
        detectSettings.isEnabled = true
    }
}
