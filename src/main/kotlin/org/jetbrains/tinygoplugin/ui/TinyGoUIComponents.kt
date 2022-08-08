package org.jetbrains.tinygoplugin.ui

import com.intellij.json.JsonFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.textFieldWithHistoryWithBrowseButton
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.createTargetWrapper
import org.jetbrains.tinygoplugin.configuration.serialize
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkChooserCombo
import org.jetbrains.tinygoplugin.sdk.nullSdk
import java.awt.event.ItemEvent
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel

fun generateTinyGoParametersPanel(
    wrapper: TinyGoPropertiesWrapper,
    parent: Disposable,
): JPanel = panel {
    tinyGoSettings(this, wrapper, parent)
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
                property.set(it.item as TinyGoSdk)
            }
        }
    }
}

fun tinyGoSdkComboChooser(
    row: Row,
    property: GraphProperty<TinyGoSdk>,
    parentDisposable: Disposable,
): Cell<TinyGoSdkChooserCombo> = with(row) {
    cell(TinyGoSdkChooserCombo())
        .horizontalAlign(HorizontalAlign.FILL)
        .applyToComponent {
            var sdk: TinyGoSdk = property.get()
            if (sdk == nullSdk) {
                selectFirstNotNullSdk()
                sdk = comboBox.model.selectedItem as TinyGoSdk
                property.set(sdk)
            } else selectSdkByUrl(sdk.homeUrl)
            Disposer.register(parentDisposable, this)
        }
        .bind(
            { component: TinyGoSdkChooserCombo -> component.sdk },
            { component: TinyGoSdkChooserCombo, value: TinyGoSdk -> component.selectSdkByUrl(value.homeUrl) },
            UIPropertyAdapter(property)
        )
        .applyToComponent { bind(property) }
}

private const val SDK_LABEL = "ui.sdk"
private const val TARGET_LABEL = "ui.target"
private const val COMPILER_PARAMETERS_LABEL = "ui.compiler"
private const val HELP_AUTO = "ui.help.auto"
private const val GC_LABEL = "ui.gc"
private const val SCHEDULER_LABEL = "ui.scheduler"
private const val TARGET_BROWSE_DIALOG_TITLE = "ui.target.dialogTitle"

private fun tinyGoSettings(
    panel: Panel,
    wrapper: TinyGoPropertiesWrapper,
    parentDisposable: Disposable,
) {
    with(panel) {
        lateinit var tinyGoSdkComboChooser: Cell<TinyGoSdkChooserCombo>
        row(TinyGoBundle.message(SDK_LABEL)) {
            tinyGoSdkComboChooser = tinyGoSdkComboChooser(this, wrapper.tinyGoSdkPath, parentDisposable)
        }
        panel {
            group(TinyGoBundle.message(COMPILER_PARAMETERS_LABEL)) {
                row(TinyGoBundle.message(TARGET_LABEL)) {
                    targetChooser(this, wrapper, tinyGoSdkComboChooser)
                }
                row(TinyGoBundle.message(GC_LABEL)) {
                    comboBox(GarbageCollector.values().toSet())
                        .bindItem(wrapper.gc)
                        .columns(COLUMNS_SHORT)
                    autoHelpLabel(this)
                }
                row(TinyGoBundle.message(SCHEDULER_LABEL)) {
                    comboBox(Scheduler.values().toSet())
                        .bindItem(wrapper.scheduler)
                        .columns(COLUMNS_SHORT)
                    autoHelpLabel(this)
                }
                row {
                    exportButton(this, wrapper)
                }
            }
        }
    }
}

private fun targetChooser(
    row: Row,
    wrapper: TinyGoPropertiesWrapper,
    sdk: Cell<TinyGoSdkChooserCombo>
) {
    with(row) {
        val jsonChooser = FileChooserDescriptor(true, false, false, false, false, false).withFileFilter {
            it.fileType == JsonFileType.INSTANCE
        }
        cell(
            textFieldWithHistoryWithBrowseButton(
                null,
                TinyGoBundle.message(TARGET_BROWSE_DIALOG_TITLE),
                jsonChooser,
            )
        ).horizontalAlign(HorizontalAlign.FILL).bind(
            { component: TextFieldWithHistoryWithBrowseButton -> component.text },
            { component: TextFieldWithHistoryWithBrowseButton, value: String -> component.text = value },
            UIPropertyAdapter(wrapper.target)
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
}

private class UIPropertyAdapter<T>(private val property: GraphProperty<T>) : MutableProperty<T> {
    override fun get(): T = property.get()

    override fun set(value: T) {
        property.set(value)
    }
}

private const val EXPORT_TARGET_BUTTON = "ui.target.export"
private const val EXPORT_TARGET_DIALOG_TITLE = "ui.target.export.dialog.title"
private const val EXPORT_TARGET_DIALOG_DESCRIPTION = "ui.target.export.dialog.description"

fun exportButton(row: Row, wrapper: TinyGoPropertiesWrapper) {
    with(row) {
        button(TinyGoBundle.message(EXPORT_TARGET_BUTTON)) {
            val target = createTargetWrapper(wrapper) ?: return@button
            val jsonChooser = FileSaverDescriptor(
                TinyGoBundle.message(EXPORT_TARGET_DIALOG_TITLE),
                TinyGoBundle.message(EXPORT_TARGET_DIALOG_DESCRIPTION),
                JsonFileType.DEFAULT_EXTENSION
            )
            val chooserDialog = service<FileChooserFactory>()
                .createSaveFileDialog(jsonChooser, null)
                .save(null)
            var file = chooserDialog?.file ?: return@button
            if (!file.name.endsWith(".json")) {
                file = File(file.path + ".json")
            }
            file.writeText(target.serialize())
        }
    }
}

fun autoHelpLabel(row: Row) {
    with(row) {
        cell(ContextHelpLabel.create(TinyGoBundle.message(HELP_AUTO)))
    }
}

fun generateSettingsPanel(
    wrapper: TinyGoPropertiesWrapper,
    parentDisposable: Disposable,
) = panel {
    tinyGoSettings(this, wrapper, parentDisposable)
    row("GOOS") {
        textField()
            .bindText(wrapper.goOs)
            .columns(COLUMNS_MEDIUM)
    }
    row("GOARCH") {
        textField()
            .bindText(wrapper.goArch)
            .columns(COLUMNS_MEDIUM)
    }
    row("Go tags") {
        expandableTextField()
            .bindText(wrapper.goTags)
            .columns(COLUMNS_LARGE)
    }
}
