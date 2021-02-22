package org.jetbrains.tinygoplugin.ui

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import org.jetbrains.tinygoplugin.configuration.ITinyGoConfiguration
import kotlin.reflect.KMutableProperty1

interface ResetableProperty {
    fun reset()
}

interface ResetableCollection {
    val resetableProperties: MutableCollection<ResetableProperty>
}

interface ConfigurationProvider<Configuration> {
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

class TinyGoPropertiesWrapper(val obj: ConfigurationProvider<ITinyGoConfiguration>) : ResetableProperty, ResetableCollection {
    // wrapper around graph property that binds the field to the property in settings
    inner class InnerGraphProperty<T>(
        prop: GraphProperty<T>,
        objProperty: KMutableProperty1<ITinyGoConfiguration, T>,
    ) : MappedGraphProperty<T, ITinyGoConfiguration>(prop, objProperty, obj, this)

    override val resetableProperties: MutableCollection<ResetableProperty> = HashSet()

    protected val propertyGraph = PropertyGraph()

    // set initial string
    val tinygoSDKPath = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::tinyGoSDKPath),
        objProperty = ITinyGoConfiguration::tinyGoSDKPath
    )
    val target = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::targetPlatform),
        objProperty = ITinyGoConfiguration::targetPlatform
    )

    val gc = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::gc),
        objProperty = ITinyGoConfiguration::gc
    )
    val scheduler = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::scheduler),
        objProperty = ITinyGoConfiguration::scheduler
    )
    val goOS = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::goOS),
        objProperty = ITinyGoConfiguration::goOS
    )
    val goArch = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::goArch),
        objProperty = ITinyGoConfiguration::goArch
    )
    val goTags = InnerGraphProperty(
        prop = propertyGraph.graphProperty(obj.tinyGoSettings::goTags),
        objProperty = ITinyGoConfiguration::goTags
    )

    override fun reset() {
        resetableProperties.forEach(ResetableProperty::reset)
    }
}
