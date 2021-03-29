package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import com.sun.java.accessibility.util.AWTEventMonitor.addItemListener
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkChooserCombo
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

private fun LayoutBuilder.tinyGoSettings(
    wrapper: TinyGoPropertiesWrapper,
) {
    row("TinyGo SDK") {
        tinyGoSdkComboChooser(property = wrapper.tinygoSDKPath)
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
