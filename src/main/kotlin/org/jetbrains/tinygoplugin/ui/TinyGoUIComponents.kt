package org.jetbrains.tinygoplugin.ui

import com.intellij.json.JsonFileType
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.observable.properties.GraphProperty
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
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkChooserCombo
import org.jetbrains.tinygoplugin.services.tinygoTargets
import java.awt.event.ItemEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel
import kotlin.reflect.KFunction

fun generateTinyGoParametersPanel(
    wrapper: TinyGoPropertiesWrapper,
): JPanel = panel {
    tinyGoSettings(wrapper)
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

fun Cell.tinyGoSdkComboChooser(property: GraphProperty<TinyGoSdk>): CellBuilder<TinyGoSdkChooserCombo> {
    return component(TinyGoSdkChooserCombo())
        .applyToComponent {
            selectSdkByUrl(property.get().homeUrl)
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
private const val GC_LABEL = "ui.gc"
private const val SCHEDULER_LABEL = "ui.scheduler"
private fun LayoutBuilder.tinyGoSettings(
    wrapper: TinyGoPropertiesWrapper,
) {
    lateinit var tinyGoSdkComboChooser: CellBuilder<TinyGoSdkChooserCombo>
    row(TinyGoBundle.message(SDK_LABEL)) {
        tinyGoSdkComboChooser = tinyGoSdkComboChooser(property = wrapper.tinygoSDKPath)
    }
    row(TinyGoBundle.message(COMPILER_PARAMETERS_LABEL)) {
        row(TinyGoBundle.message(TARGET_LABEL)) {
            targetChooser(wrapper, tinyGoSdkComboChooser)
        }
        row(TinyGoBundle.message(GC_LABEL)) {
            comboBox(EnumComboBoxModel(GarbageCollector::class.java), wrapper.gc)
        }
        row(TinyGoBundle.message(SCHEDULER_LABEL)) {
            comboBox(EnumComboBoxModel(Scheduler::class.java), wrapper.scheduler)
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
        "Select target file",
        null,
        jsonChooser,
    ).applyToComponent {
        text = wrapper.target.get()
        wrapper.target.afterChange {
            text = it
        }
        childComponent.addActionListener {
            if (childComponent.isShowing) {
                wrapper.target.set(text)
            }
        }
        childComponent.history = tinygoTargets(wrapper.tinygoSDKPath.get())
        sdk.component.addChangedListener {
            childComponent.history = tinygoTargets(sdk.component.sdk)
        }
    }
}

fun generateSettingsPanel(
    wrapper: TinyGoPropertiesWrapper,
    onDetect: KFunction<Unit>,
    onPropagateGoTags: KFunction<Unit>,
) = panel {
    row {
        tinyGoSettings(wrapper)
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
