package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.services.tinygoTargets

class ConfigurationWithHistory(
    val settings: TinyGoConfiguration = TinyGoConfiguration.getInstance(),
    private val pathConverter: PathConverter = object : PathConverter {},
) :
    TinyGoConfiguration by settings {
    constructor(project: Project) : this(TinyGoConfiguration.getInstance(project).deepCopy(),
        ProjectPathConverter(project))

    override var sdk: TinyGoSdk
        get() = settings.sdk
        set(value) {
            if (value != settings.sdk) {
                settings.sdk = value
                predefinedTargets = tinygoTargets(value)
            }
        }
    override var targetPlatform: String
        get() {
            if (predefinedTargets.contains(settings.targetPlatform)) {
                return settings.targetPlatform
            }
            val absolutePath = pathConverter.toAbsolute(settings.targetPlatform)
            return absolutePath.ifEmpty { settings.targetPlatform }
        }
        set(value) {
            var newTarget = value
            if (!predefinedTargets.contains(newTarget)) {
                newTarget = pathConverter.toRelative(value).ifEmpty { value }
                if (!userTargets.contains(newTarget)) {
                    settings.userTargets += newTarget
                }
            }
            settings.targetPlatform = newTarget
        }
    override var userTargets: List<String>
        get() = settings.userTargets.map { pathConverter.toAbsolute(it) }
            .filter(String::isNotEmpty) + predefinedTargets
        set(value) {
            settings.userTargets = value
        }
    var predefinedTargets: List<String> = tinygoTargets(settings.sdk)
}

