package org.jetbrains.tinygoplugin.ui

import com.intellij.json.JsonFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.createTargetWrapper
import org.jetbrains.tinygoplugin.configuration.serialize
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkChooserCombo
import org.jetbrains.tinygoplugin.sdk.nullSdk
import java.awt.event.ItemEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton
import javax.swing.JPanel

fun generateTinyGoParametersPanel(
    wrapper: TinyGoPropertiesWrapper,
    parent: Disposable,
): JPanel = panel {
    tinyGoSettings(wrapper, parent)
}

private fun AtomicBoolean.lockOrSkip(action: () -> Unit) {
    if (!compareAndSet(false, true)) return
    try {
        action()
    } finally {
        set(false)
    }
}

fun TinyGoSdkChooserCombo.bind(property: GraphProperty<TinyGoSdk>) {
    val mutex = AtomicBoolean()
    property.afterChange {
        mutex.lockOrSkip {
            selectSdkByUrl(it.homeUrl)
        }
    }

    childComponent.addItemListener {
        if (it.stateChange == ItemEvent.SELECTED) {
            mutex.lockOrSkip {
                @Suppress("UNCHECKED_CAST")
                property.set(it.item as TinyGoSdk)
            }
        }
    }
}

fun Cell.tinyGoSdkComboChooser(
    property: GraphProperty<TinyGoSdk>,
    parentDisposable: Disposable,
): CellBuilder<TinyGoSdkChooserCombo> {
    return component(TinyGoSdkChooserCombo())
        .applyToComponent {
            var sdk: TinyGoSdk = property.get()
            if (sdk == nullSdk) {
                selectFirstNotNullSdk()
                @Suppress("UnstableApiUsage")
                sdk = comboBox.model.selectedItem as TinyGoSdk
                property.set(sdk)
            } else selectSdkByUrl(sdk.homeUrl)
            Disposer.register(parentDisposable, this)
        }
        .withBinding(
            { component -> component.sdk },
            { component, value -> component.selectSdkByUrl(value.homeUrl) },
            PropertyBinding(property::get, property::set)
        )
        .withGraphProperty(property)
        .applyToComponent { bind(property) }
}

private const val SDK_LABEL = "ui.sdk"
private const val TARGET_LABEL = "ui.target"
private const val COMPILER_PARAMETERS_LABEL = "ui.compiler"
private const val HELP_AUTO = "ui.help.auto"
private const val GC_LABEL = "ui.gc"
private const val SCHEDULER_LABEL = "ui.scheduler"
private const val TARGET_BROWSE_DIALOG_TITLE = "ui.target.dialogTitle"

private fun LayoutBuilder.tinyGoSettings(
    wrapper: TinyGoPropertiesWrapper,
    parentDisposable: Disposable,
) {
    lateinit var tinyGoSdkComboChooser: CellBuilder<TinyGoSdkChooserCombo>
    row(TinyGoBundle.message(SDK_LABEL)) {
        tinyGoSdkComboChooser = tinyGoSdkComboChooser(property = wrapper.tinyGoSdkPath, parentDisposable)
    }
    titledRow(TinyGoBundle.message(COMPILER_PARAMETERS_LABEL)) {
        row(TinyGoBundle.message(TARGET_LABEL)) {
            targetChooser(wrapper, tinyGoSdkComboChooser)
        }
        row(TinyGoBundle.message(GC_LABEL)) {
            comboBox(EnumComboBoxModel(GarbageCollector::class.java), wrapper.gc)
            autoHelpLabel()
        }
        row(TinyGoBundle.message(SCHEDULER_LABEL)) {
            comboBox(EnumComboBoxModel(Scheduler::class.java), wrapper.scheduler)
            autoHelpLabel()
        }
        row {
            exportButton(wrapper)
        }
    }
}

private fun Row.targetChooser(wrapper: TinyGoPropertiesWrapper, sdk: CellBuilder<TinyGoSdkChooserCombo>) {
    val jsonChooser = FileChooserDescriptor(true, false, false, false, false, false).withFileFilter {
        it.fileType == JsonFileType.INSTANCE
    }
    textFieldWithHistoryWithBrowseButton(
        { wrapper.target.get() },
        { wrapper.target.set(it) },
        TinyGoBundle.message(TARGET_BROWSE_DIALOG_TITLE),
        null,
        jsonChooser,
    ).applyToComponent {
        text = wrapper.target.get()
        wrapper.target.afterChange {
            text = it
            val historyIndex = childComponent.history.indexOf(it)
            if (historyIndex >= 0) {
                childComponent.selectedIndex = historyIndex
            }
        }
        childComponent.addActionListener {
            if (childComponent.isShowing) {
                wrapper.target.set(text)
            }
        }
        childComponent.history = wrapper.userTargets
        sdk.component.addChangedListener {
            childComponent.history = wrapper.userTargets
        }
    }
}

fun Cell.exportButton(wrapper: TinyGoPropertiesWrapper) {
    component(JButton("Export TinyGo Target...")).applyToComponent {
        this.addActionListener {
            val target = createTargetWrapper(wrapper) ?: return@addActionListener
            val jsonChooser = FileSaverDescriptor(
                "Save TinyGo Target As...",
                "Select a JSON file to export your custom TinyGo target",
                JsonFileType.DEFAULT_EXTENSION
            )
            val chooserDialog =
                FileChooserFactory.getInstance().createSaveFileDialog(jsonChooser, null).save(null)
            chooserDialog?.file?.writeText(target.serialize())
        }
    }.growPolicy(GrowPolicy.MEDIUM_TEXT)
}

fun Row.autoHelpLabel() {
    right { component(ContextHelpLabel.create(TinyGoBundle.message(HELP_AUTO))) }
}

fun generateSettingsPanel(
    wrapper: TinyGoPropertiesWrapper,
    parentDisposable: Disposable,
) = panel {
    row {
        tinyGoSettings(wrapper, parentDisposable)
    }
    row("GOOS") {
        textField(property = wrapper.goOs).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    row("GOARCH") {
        textField(property = wrapper.goArch).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
    row("Go tags") {
        expandableTextField(wrapper.goTags).growPolicy(GrowPolicy.MEDIUM_TEXT)
    }
}
