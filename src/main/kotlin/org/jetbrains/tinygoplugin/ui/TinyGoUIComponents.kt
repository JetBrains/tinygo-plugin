package org.jetbrains.tinygoplugin.ui

import com.intellij.json.JsonFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.textFieldWithHistoryWithBrowseButton
import com.intellij.ui.dsl.builder.Align
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.createTargetWrapper
import org.jetbrains.tinygoplugin.configuration.serialize
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkChooserCombo
import org.jetbrains.tinygoplugin.sdk.nullSdk
import org.jetbrains.tinygoplugin.services.TinyGoServiceScope
import java.awt.event.ItemEvent
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel

fun generateTinyGoNewProjectSettingsPanel(
    project: Project?,
    projectPathSupplier: () -> String,
    wrapper: TinyGoPropertiesWrapper,
    parent: Disposable,
): JPanel = panel {
    tinyGoSettings(project, projectPathSupplier, wrapper, parent)
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

fun Row.tinyGoSdkComboChooser(
    projectPathSupplier: () -> String,
    property: GraphProperty<TinyGoSdk>,
    parentDisposable: Disposable,
): Cell<TinyGoSdkChooserCombo> {
    return cell(TinyGoSdkChooserCombo(projectPathSupplier))
        .align(Align.FILL)
        .applyToComponent {
            TinyGoServiceScope.getScope().launch(ModalityState.current().asContextElement()) {
                var sdk: TinyGoSdk = property.get()
                if (sdk == nullSdk) {
                    selectFirstNotNullSdk()
                    sdk = comboBox.model.selectedItem as TinyGoSdk
                    withContext(Dispatchers.EDT) {
                        property.set(sdk)
                    }
                } else selectSdkByUrl(sdk.homeUrl)
            }
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
private const val TARGET_LABEL = "ui.target.platform"
private const val COMPILER_PARAMETERS_LABEL = "ui.compiler"
private const val HELP_AUTO = "ui.help.auto"
private const val GC_LABEL = "ui.gc"
private const val SCHEDULER_LABEL = "ui.scheduler"
private const val TARGET_BROWSE_DIALOG_TITLE = "ui.target.dialogTitle"

private fun Panel.tinyGoSettings(
    project: Project?,
    projectPathSupplier: () -> String,
    wrapper: TinyGoPropertiesWrapper,
    parentDisposable: Disposable,
) {
    lateinit var tinyGoSdkComboChooser: Cell<TinyGoSdkChooserCombo>
    row(TinyGoBundle.message(SDK_LABEL)) {
        tinyGoSdkComboChooser = tinyGoSdkComboChooser(
            projectPathSupplier,
            wrapper.tinyGoSdkPath,
            parentDisposable
        )
    }
    panel {
        group(TinyGoBundle.message(COMPILER_PARAMETERS_LABEL)) {
            row(TinyGoBundle.message(TARGET_LABEL)) {
                targetChooser(project, wrapper, tinyGoSdkComboChooser)
            }
            row(TinyGoBundle.message(GC_LABEL)) {
                comboBox(GarbageCollector.entries.toSet())
                    .bindItem(wrapper.gc)
                    .columns(COLUMNS_SHORT)
                autoHelpLabel()
            }
            row(TinyGoBundle.message(SCHEDULER_LABEL)) {
                comboBox(Scheduler.entries.toSet())
                    .bindItem(wrapper.scheduler)
                    .columns(COLUMNS_SHORT)
                autoHelpLabel()
            }
            row {
                exportButton(project, wrapper)
            }
        }
    }
}

private fun Row.targetChooser(
    project: Project?,
    wrapper: TinyGoPropertiesWrapper,
    sdk: Cell<TinyGoSdkChooserCombo>
) {
    val jsonChooser = FileChooserDescriptor(true, false, false, false, false, false)
        .withFileFilter { it.fileType == JsonFileType.INSTANCE }
        .withTitle(TinyGoBundle.message(TARGET_BROWSE_DIALOG_TITLE))
    cell(
        textFieldWithHistoryWithBrowseButton(
            project,
            jsonChooser,
        )
    ).align(Align.FILL).bind(
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
        childComponent.history = runWithModalProgressBlocking(
            ModalTaskOwner.component(this),
            TinyGoBundle.message("ui.target.loading.title")
        ) {
            readAction { wrapper.userTargets }
        }
        sdk.component.addChangedListener {
            childComponent.history = runWithModalProgressBlocking(
                ModalTaskOwner.component(this),
                TinyGoBundle.message("ui.target.loading.title")
            ) {
                readAction { wrapper.userTargets }
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

fun Row.exportButton(project: Project?, wrapper: TinyGoPropertiesWrapper) {
    button(TinyGoBundle.message(EXPORT_TARGET_BUTTON)) {
        TinyGoServiceScope.getScope(project).launch(ModalityState.current().asContextElement()) {
            val target = createTargetWrapper(wrapper) ?: return@launch
            val jsonChooser = FileSaverDescriptor(
                TinyGoBundle.message(EXPORT_TARGET_DIALOG_TITLE),
                TinyGoBundle.message(EXPORT_TARGET_DIALOG_DESCRIPTION),
                JsonFileType.DEFAULT_EXTENSION
            )
            var file = withContext(Dispatchers.EDT) {
                val chooserDialog = service<FileChooserFactory>()
                    .createSaveFileDialog(jsonChooser, null)
                    .save(null)
                chooserDialog?.file
            } ?: return@launch
            if (!file.name.endsWith(".json")) {
                file = File(file.path + ".json")
            }
            withContext(Dispatchers.IO) {
                file.writeText(target.serialize())
            }
        }
    }
}

fun Row.autoHelpLabel() {
    cell(ContextHelpLabel.create(TinyGoBundle.message(HELP_AUTO)))
}

fun generateSettingsPanel(
    project: Project,
    wrapper: TinyGoPropertiesWrapper,
    parentDisposable: Disposable,
) = panel {
    tinyGoSettings(project, { project.basePath.orEmpty() }, wrapper, parentDisposable)
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
