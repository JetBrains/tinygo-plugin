package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkChooserCombo
import javax.swing.JPanel
import kotlin.reflect.KFunction

fun generateTinyGoParametersPanel(
    wrapper: TinyGoPropertiesWrapper,
): JPanel = panel {
    tinyGoSettings(wrapper)
}

fun Cell.tinyGoSdkComboChooser(property: GraphProperty<TinyGoSdk>): CellBuilder<TinyGoSdkChooserCombo> {
    return component(TinyGoSdkChooserCombo()).withBinding(
        { component -> component.sdk },
        { component, value -> component.selectSdkByUrl(value.homeUrl) },
        PropertyBinding(property::get, property::set)
    )
        .withGraphProperty(property)
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
