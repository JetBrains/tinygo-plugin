package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import javax.swing.JPanel
import kotlin.reflect.KFunction

fun generateTinyGoParametersPanel(
    wrapper: TinyGoPropertiesWrapper,
    fileChosen: ((chosenFile: VirtualFile) -> String),
    project: Project? = null,
): JPanel = panel {
    tinyGoSettings(wrapper, fileChosen, project)
}

private fun LayoutBuilder.tinyGoSettings(
    wrapper: TinyGoPropertiesWrapper,
    fileChosen: (chosenFile: VirtualFile) -> String,
    project: Project?,
) {
    row("TinyGo Path") {
        textFieldWithBrowseButton(
            property = wrapper.tinygoSDKPath, project = project,
            fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            fileChosen = fileChosen
        )
    }
    row("Target platform") {
        textField(property = wrapper.target).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    row("Compiler parameters:") {
        row("Garbage collector") {
            comboBox(EnumComboBoxModel(GarbageCollector::class.java), wrapper.gc)
        }
        row("Scheduler") {
            comboBox(EnumComboBoxModel(Scheduler::class.java), wrapper.scheduler)
        }
    }
}

fun generateSettingsPanel(
    wrapper: TinyGoPropertiesWrapper,
    fileChosen: ((chosenFile: VirtualFile) -> String),
    onDetect: KFunction<Unit>,
    onPropagateGoTags: KFunction<Unit>,
    project: Project? = null,
): JPanel = panel {
    row {
        tinyGoSettings(wrapper, fileChosen, project)
    }
    row {
        button("Detect") { onDetect.call() /*extractTinyGOParameters()*/ }
        button("Update gopath") { onPropagateGoTags.call() /*propagateGoFlags()*/ }
    }
    row("GOOS") {
        textField(property = wrapper.goOS).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    row("GOARCH") {
        textField(property = wrapper.goArch).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    row("Go tags") {
        textField(property = wrapper.goTags).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
}
