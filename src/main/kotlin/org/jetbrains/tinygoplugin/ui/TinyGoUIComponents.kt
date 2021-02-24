package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.enableIf
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected
import org.jetbrains.annotations.Nls
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import javax.swing.JPanel
import kotlin.reflect.KFunction

private fun LayoutBuilder.filteredRow(
    @Nls label: String? = null,
    separated: Boolean = false,
    filter: ComponentPredicate? = null,
    init: Row.() -> Unit,
): Row {
    val result = row(
        label = label,
        separated = separated,
        init = init
    )
    if (filter != null) {
        result.enableIf(filter)
    }
    return result
}

fun generateTinyGoParametersPanel(
    wrapper: TinyGoPropertiesWrapper,
    fileChosen: ((chosenFile: VirtualFile) -> String),
    project: Project? = null,
    filter: ComponentPredicate? = null,
): JPanel = panel {
    tinyGoSettings(wrapper, fileChosen, project, filter)
}

private fun LayoutBuilder.tinyGoSettings(
    wrapper: TinyGoPropertiesWrapper,
    fileChosen: (chosenFile: VirtualFile) -> String,
    project: Project?,
    filter: ComponentPredicate?,
) {
    filteredRow("TinyGo Path", filter = filter) {
        textFieldWithBrowseButton(
            property = wrapper.tinygoSDKPath, project = project,
            fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            fileChosen = fileChosen
        )
    }
    filteredRow("Target platform", filter = filter) {
        textField(property = wrapper.target).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    filteredRow("Compiler parameters:", filter = filter) {
        filteredRow("Garbage collector", filter = filter) {
            comboBox(EnumComboBoxModel(GarbageCollector::class.java), wrapper.gc)
        }
        filteredRow("Scheduler", filter = filter) {
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
    lateinit var tinyGoEnabled: ComponentPredicate
    row {
        val enabledCheckbox = checkBox("TinyGo enabled", property = wrapper.tinyGoEnabled)
        tinyGoEnabled = enabledCheckbox.selected
    }
    tinyGoSettings(wrapper, fileChosen, project, tinyGoEnabled)
    filteredRow(filter = tinyGoEnabled) {
        button("Detect") { onDetect.call() /*extractTinyGOParameters()*/ }
        button("Update gopath") { onPropagateGoTags.call() /*propagateGoFlags()*/ }
    }
    filteredRow("GOOS", filter = tinyGoEnabled) {
        textField(property = wrapper.goOS).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    filteredRow("GOARCH", filter = tinyGoEnabled) {
        textField(property = wrapper.goArch).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    filteredRow("Go tags", filter = tinyGoEnabled) {
        textField(property = wrapper.goTags).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
}
