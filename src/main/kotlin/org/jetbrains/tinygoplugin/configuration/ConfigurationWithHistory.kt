package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.services.TinyGoServiceScope
import org.jetbrains.tinygoplugin.services.tinyGoTargets
class ConfigurationWithHistory(
    val settings: TinyGoConfiguration = TinyGoConfiguration.getInstance(),
    private val maintainPredefinedTargets: Boolean = false,
) :
    TinyGoConfiguration by settings {
    constructor(project: Project, maintainPredefinedTargets: Boolean = false) : this(
        project.tinyGoConfiguration(),
        maintainPredefinedTargets,
    ) {
        if (maintainPredefinedTargets) updatePredefinedTargets()
    }

    override var sdk: TinyGoSdk
        get() = settings.sdk
        set(value) {
            if (value != settings.sdk) {
                settings.sdk = value
                if (maintainPredefinedTargets) updatePredefinedTargets()
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
        val result = ConfigurationWithHistory(settingsCopy, maintainPredefinedTargets)
        result.predefinedTargets = predefinedTargets
        return result
    }

    @Volatile
    var predefinedTargets: Set<String> = emptySet()

    private fun updatePredefinedTargets() {
        TinyGoServiceScope.getScope().launch(Dispatchers.IO) {
            val isValid = settings.sdk.refreshValidity()
            val sdkPath = if (isValid) readAction { settings.sdk.sdkRoot?.toNioPath() } else null
            predefinedTargets = if (sdkPath == null) emptySet() else tinyGoTargets(sdkPath)
        }
    }
}
