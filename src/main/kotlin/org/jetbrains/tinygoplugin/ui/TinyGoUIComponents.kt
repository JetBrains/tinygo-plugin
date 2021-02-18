package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import java.awt.event.ActionEvent
import javax.swing.JPanel

class TinyGoUIComponents private constructor() {
    companion object {
        fun generateTinyGoParametersPanel(
            wrapper: TinyGoPropertiesWrapper,
            fileChosen: ((chosenFile: VirtualFile) -> String),
            project: Project? = null
        ): JPanel = panel {
            row("TinyGo Path") {
                textFieldWithBrowseButton(
                    property = wrapper.tinygoSDKPath, project = project,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    fileChosen = fileChosen
                )
            }
            row("Target platform") {
                textField(property = wrapper.target)
            }
            row("Compiler parameters:") {
                comboBox(EnumComboBoxModel(GarbageCollector::class.java), wrapper.gc)
                comboBox(EnumComboBoxModel(Scheduler::class.java), wrapper.scheduler)
            }
        }

        fun generateSettingsPanel(
            wrapper: TinyGoPropertiesWrapper,
            fileChosen: ((chosenFile: VirtualFile) -> String),
            actionPerformed: ((event: ActionEvent) -> Unit),
            project: Project? = null
        ): JPanel = panel {
            row {
                generateTinyGoParametersPanel(wrapper, fileChosen, project)()
            }
            row {
                button("Detect", actionPerformed)
            }
            row("GOOS") {
                textField(property = wrapper.goOS).enabled(false)
            }
            row("GOARCH") {
                textField(property = wrapper.goArch).enabled(false)
            }
            row("Go tags") {
                textField(property = wrapper.goTags).enabled(false)
            }
        }
    }
}
