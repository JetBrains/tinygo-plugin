package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.*
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import java.awt.event.ActionEvent
import javax.swing.JPanel

class TinyGoUIComponents private constructor() {
    companion object {
        fun generateTinyGoParametersPanel(
            tinyGoSDKPath: GraphProperty<String>,
            fileChosen: ((chosenFile: VirtualFile) -> String),
            target: GraphProperty<String>,
            gc: GraphProperty<GarbageCollector>,
            scheduler: GraphProperty<Scheduler>,
            project: Project? = null
        ): JPanel = panel {
            row("TinyGo Path") {
                textFieldWithBrowseButton(
                    property = tinyGoSDKPath, project = project,
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    fileChosen = fileChosen
                )
            }
            row("Target platform") {
                textField(property = target)
            }
            row("Compiler parameters:") {
                comboBox(EnumComboBoxModel(GarbageCollector::class.java), gc)
                comboBox(EnumComboBoxModel(Scheduler::class.java), scheduler)
            }
        }

        fun generateSettingsPanel(
            tinyGoSDKPath: GraphProperty<String>,
            fileChosen: ((chosenFile: VirtualFile) -> String),
            target: GraphProperty<String>,
            gc: GraphProperty<GarbageCollector>,
            scheduler: GraphProperty<Scheduler>,
            actionPerformed: ((event: ActionEvent) -> Unit),
            goOS: GraphProperty<String>,
            goArch: GraphProperty<String>,
            goTags: GraphProperty<String>,
            project: Project? = null
        ): JPanel = panel {
            row {
                generateTinyGoParametersPanel(tinyGoSDKPath, fileChosen, target, gc, scheduler, project)()
            }
            row {
                button("Detect", actionPerformed)
            }
            row("GOOS") {
                textField(property = goOS).enabled(false)
            }
            row("GOARCH") {
                textField(property = goArch).enabled(false)
            }
            row("Go tags") {
                textField(property = goTags).enabled(false)
            }
        }
    }
}
