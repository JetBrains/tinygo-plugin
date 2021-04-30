package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.services.tinyGoTargets
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LazyVar<T : Any>(private val initializer: () -> T) : ReadWriteProperty<Any?, T> {
    private lateinit var value: T

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!this::value.isInitialized) {
            value = initializer()
        }
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

fun <T : Any> lazyVar(initializer: () -> T): LazyVar<T> = LazyVar(initializer)

class ConfigurationWithHistory(
    val settings: TinyGoConfiguration = TinyGoConfiguration.getInstance(),
) :
    TinyGoConfiguration by settings {
    constructor(project: Project) : this(
        TinyGoConfiguration.getInstance(project),
    )

    override var sdk: TinyGoSdk
        get() = settings.sdk
        set(value) {
            if (value != settings.sdk) {
                settings.sdk = value
                predefinedTargets = tinyGoTargets(value)
            }
        }

    override var targetPlatform: String
        get() = settings.targetPlatform
        set(value) {
            if (!predefinedTargets.contains(value) && !settings.userTargets.contains(value)) {
                settings.userTargets += value
            }
            settings.targetPlatform = value
        }

    override var userTargets: List<String>
        get() = settings.userTargets + predefinedTargets
        set(value) {
            settings.userTargets = value
        }

    override fun deepCopy(): TinyGoConfiguration {
        val settingsCopy = settings.deepCopy()
        val result = ConfigurationWithHistory(settingsCopy)
        result.predefinedTargets = predefinedTargets
        return result
    }

    var predefinedTargets: Set<String> by lazyVar {
        tinyGoTargets(settings.sdk)
    }
}
