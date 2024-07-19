package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.tinygoplugin.configuration.CachedGoRootInvalidator
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.sendReloadLibrariesSignal
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateSettingsPanel

class TinyGoConfigurationWithTagUpdate(
    private val settings: TinyGoConfiguration,
    project: Project,
    private val callback: () -> Unit,
) :
    TinyGoConfiguration by settings {

    constructor(project: Project, callback: () -> Unit) : this(ConfigurationWithHistory(project), project, callback)

    init {
        val moduleSettings = goSettings(project)
        if (moduleSettings == null) {
            thisLogger().warn("Could not find go module settings")
        } else {
            val buildSettings = moduleSettings.buildTargetSettings
            settings.goArch = buildSettings.arch
            settings.goOS = buildSettings.os
            settings.goTags = buildSettings.customFlags.joinToString(" ")
        }
    }

    override var sdk: TinyGoSdk
        get() = settings.sdk
        set(value) {
            if (settings.sdk != value) {
                settings.sdk = value
                callback()
            }
        }

    override var targetPlatform: String
        get() = settings.targetPlatform
        set(value) {
            if (settings.targetPlatform != value) {
                settings.targetPlatform = value
                if (value.isNotEmpty()) {
                    settings.gc = GarbageCollector.AUTO_DETECT
                    settings.scheduler = Scheduler.AUTO_DETECT
                    callback()
                }
            }
        }

    override var gc: GarbageCollector
        get() = settings.gc
        set(value) {
            if (settings.gc != value) {
                settings.gc = value
                callback()
            }
        }

    override var scheduler: Scheduler
        get() = settings.scheduler
        set(value) {
            if (settings.scheduler != value) {
                settings.scheduler = value
                callback()
            }
        }

    private fun goSettings(project: Project): GoModuleSettings? =
        ModuleManager.getInstance(project).modules.firstNotNullOfOrNull {
            it.getService(GoModuleSettings::class.java)
        }

    override fun modified(project: Project): Boolean {
        val moduleSettings = goSettings(project)
        if (moduleSettings != null) {
            val buildSettings = moduleSettings.buildTargetSettings
            if (settings.goArch != buildSettings.arch ||
                settings.goOS != buildSettings.os ||
                settings.goTags != buildSettings.customFlags.joinToString(" ")
            ) {
                return true
            }
        }
        return settings.modified(project)
    }
}

class TinyGoSettingsService(private val project: Project) :
    BoundConfigurable("TinyGo"), ConfigurationProvider<TinyGoConfiguration> {
    // local copy of the settings
    override var tinyGoSettings: TinyGoConfiguration =
        TinyGoConfigurationWithTagUpdate(project, this::callExtractor)

    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun isModified(): Boolean = tinyGoSettings.modified(project)

    override fun apply() {
        thisLogger().warn("Apply called")
        val oldConfiguration = project.tinyGoConfiguration()
        val oldSdk = oldConfiguration.sdk
        val oldTarget = oldConfiguration.targetPlatform
        tinyGoSettings.saveState(project)
        propagateGoFlags()
        updateTinyGoRunConfigurations()
        if (oldSdk != tinyGoSettings.sdk || oldTarget != tinyGoSettings.targetPlatform) {
            sendReloadLibrariesSignal(project)
        }
    }

    override fun createPanel(): DialogPanel = generateSettingsPanel(project, propertiesWrapper, disposable!!)

    private fun callExtractor() {
        project.service<TinyGoInfoExtractor>()
            .extractTinyGoInfo(tinyGoSettings, CachedGoRootInvalidator(project)) { _, output ->
                thisLogger().trace(output)
                TinyGoServiceScope.getScope(project).launch(ModalityState.current().asContextElement()) {
                    tinyGoSettings.extractTinyGoInfo(output)
                    withContext(Dispatchers.EDT) {
                        // update all ui fields
                        propertiesWrapper.reset()
                    }
                }
            }
    }

    override fun reset() {
        tinyGoSettings = TinyGoConfigurationWithTagUpdate(project, this::callExtractor)
        propertiesWrapper.reset()
        super.reset()
    }

    private fun propagateGoFlags() {
        propagateGoFlags(project, tinyGoSettings)
    }

    private fun updateTinyGoRunConfigurations() {
        updateTinyGoRunConfigurations(project, tinyGoSettings)
    }
}
