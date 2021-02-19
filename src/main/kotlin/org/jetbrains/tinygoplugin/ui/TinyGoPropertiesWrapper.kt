package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import kotlin.reflect.KMutableProperty1

interface ResetableProperty {
    fun reset()
}

interface SettingsProvider {
    val settings: TinyGoConfiguration
}

interface CanResetSettingsUI : SettingsProvider {
    val resetableProperties: MutableCollection<ResetableProperty>
}

class TinyGoPropertiesWrapper(val obj: SettingsProvider) {
    // wrapper around graph property that binds the field to the property in settings
    inner class MappedGraphProperty<T>(
        private val prop: GraphProperty<T>,
        private val objProperty: KMutableProperty1<TinyGoConfiguration, T>,
    ) : GraphProperty<T> by prop, ResetableProperty {
        init {
            prop.afterChange {
                objProperty.set(obj.settings, it)
            }
            prop.afterReset {
                prop.set(objProperty.get(obj.settings))
            }
            if (obj is CanResetSettingsUI) {
                obj.resetableProperties.add(this)
            }
        }

        override fun reset() = prop.reset()
    }

    private val propertyGraph = PropertyGraph()

    // set initial string
    val tinygoSDKPath = MappedGraphProperty(
        prop = propertyGraph.graphProperty(obj.settings::tinyGoSDKPath),
        objProperty = TinyGoConfiguration::tinyGoSDKPath
    )
    val target = MappedGraphProperty(
        prop = propertyGraph.graphProperty(obj.settings::targetPlatform),
        objProperty = TinyGoConfiguration::targetPlatform
    )

    val gc = MappedGraphProperty(
        prop = propertyGraph.graphProperty(obj.settings::gc),
        objProperty = TinyGoConfiguration::gc
    )
    val scheduler = MappedGraphProperty(
        prop = propertyGraph.graphProperty(obj.settings::scheduler),
        objProperty = TinyGoConfiguration::scheduler
    )
    val goOS = MappedGraphProperty(
        prop = propertyGraph.graphProperty(obj.settings::goOS),
        objProperty = TinyGoConfiguration::goOS
    )
    val goArch = MappedGraphProperty(
        prop = propertyGraph.graphProperty(obj.settings::goArch),
        objProperty = TinyGoConfiguration::goArch
    )
    val goTags = MappedGraphProperty(
        prop = propertyGraph.graphProperty(obj.settings::goTags),
        objProperty = TinyGoConfiguration::goTags
    )
}
