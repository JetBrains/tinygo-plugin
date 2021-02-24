package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import kotlin.reflect.KMutableProperty1

interface ResetableProperty {
    fun reset()
}

interface ResetableCollection {
    val resetableProperties: MutableCollection<ResetableProperty>
}

interface ConfigurationProvider<out Configuration> {
    val tinyGoSettings: Configuration
}

open class MappedGraphProperty<T, Configuration>(
    private val prop: GraphProperty<T>,
    private val objProperty: KMutableProperty1<Configuration, T>,
    configuration: ConfigurationProvider<Configuration>,
    propertyAggregator: ResetableCollection,
) : GraphProperty<T> by prop, ResetableProperty {
    init {
        prop.afterChange {
            objProperty.set(configuration.tinyGoSettings, it)
        }
        prop.afterReset {
            prop.set(objProperty.get(configuration.tinyGoSettings))
        }
        propertyAggregator.resetableProperties.add(this)
    }

    override fun reset() = prop.reset()
}

open class TinyGoPropertiesWrapper(val obj: ConfigurationProvider<TinyGoConfiguration>) :
    ResetableProperty,
    ResetableCollection {
    // wrapper around graph property that binds the field to the property in settings
    inner class InnerGraphProperty<T>(
        prop: GraphProperty<T>,
        objProperty: KMutableProperty1<TinyGoConfiguration, T>,
    ) : MappedGraphProperty<T, TinyGoConfiguration>(prop, objProperty, obj, this)

    override val resetableProperties: MutableCollection<ResetableProperty> = HashSet()

    protected val propertyGraph = PropertyGraph()

    val tinyGoEnabled = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::enabled),
        objProperty = TinyGoConfiguration::enabled
    )

    // set initial string
    val tinygoSDKPath = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::tinyGoSDKPath),
        objProperty = TinyGoConfiguration::tinyGoSDKPath
    )
    val target = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::targetPlatform),
        objProperty = TinyGoConfiguration::targetPlatform
    )

    val gc = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::gc),
        objProperty = TinyGoConfiguration::gc
    )
    val scheduler = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::scheduler),
        objProperty = TinyGoConfiguration::scheduler
    )
    val goOS = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::goOS),
        objProperty = TinyGoConfiguration::goOS
    )
    val goArch = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::goArch),
        objProperty = TinyGoConfiguration::goArch
    )
    val goTags = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::goTags),
        objProperty = TinyGoConfiguration::goTags
    )

    override fun reset() {
        resetableProperties.forEach(ResetableProperty::reset)
    }
}
