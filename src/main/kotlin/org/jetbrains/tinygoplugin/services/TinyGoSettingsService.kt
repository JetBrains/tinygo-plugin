package org.jetbrains.tinygoplugin.services

import com.goide.GoLibrariesUtil
import com.goide.project.GoBuildTargetSettings
import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdkService
import com.goide.util.GoUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.util.messages.MessageBus
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
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
            TinyGoSettingsService.logger.warn("Could not find go module settings")
        } else {
            val buildSettings = moduleSettings.buildTargetSettings
            settings.goArch = buildSettings.arch
            settings.goOS = buildSettings.os
            settings.goTags = buildSettings.customFlags.joinToString(" ")
        }
    }

    override var targetPlatform: String
        get() = settings.targetPlatform
        set(value) {
            if (settings.targetPlatform != value) {
                settings.targetPlatform = value
                if (value.isNotEmpty()) {
                    callback()
                }
            }
        }

    private fun goSettings(project: Project): GoModuleSettings? =
        ModuleManager.getInstance(project).modules.mapNotNull {
            it?.getService(GoModuleSettings::class.java)
        }.firstOrNull()

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
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoSettingsService::class.java)
    }

    // local copy of the settings
    override var tinyGoSettings: TinyGoConfiguration =
        TinyGoConfigurationWithTagUpdate(project, this::callExtractor)

    private val infoExtractor = TinyGoInfoExtractor(project)
    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun isModified(): Boolean = tinyGoSettings.modified(project)

    override fun apply() {
        logger.warn("Apply called")
        val oldSdk = TinyGoConfiguration.getInstance(project).sdk
        tinyGoSettings.saveState(project)
        propagateGoFlags()
        if (oldSdk != tinyGoSettings.sdk) {
            if (!project.isDisposed) {
                ApplicationManager.getApplication().assertIsDispatchThread()
                GoSdkService.getInstance(project).incModificationCount()
                GoUtil.cleanResolveCache(project)
                GoLibrariesUtil.updateLibraries(project, EmptyRunnable.getInstance(), null)
                val messageBus: MessageBus = project.messageBus
                val modules = ModuleManager.getInstance(project).modules
                modules.filter {
                    GoBuildTargetSettings.DEFAULT == GoModuleSettings.getInstance(it).buildTargetSettings.goVersion
                }.forEach { messageBus.syncPublisher(GoModuleSettings.BUILD_TARGET_TOPIC).changed(it, true) }
            }
        }
    }

    override fun createPanel(): DialogPanel = generateSettingsPanel(propertiesWrapper, disposable!!)

    private fun callExtractor() {
        infoExtractor.extractTinyGoInfo(tinyGoSettings) { _, output ->
            logger.trace(output)
            tinyGoSettings.extractTinyGoInfo(output)
            // update all ui fields
            propertiesWrapper.reset()
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
}

fun propagateGoFlags(project: Project, settings: TinyGoConfiguration) {
    val goSettings = ModuleManager.getInstance(project).modules.mapNotNull {
        it?.getService(GoModuleSettings::class.java)
    }.firstOrNull()
    if (goSettings == null) {
        TinyGoSettingsService.logger.warn("Could not find go module settings")
        return
    }
    val buildSettings = goSettings.buildTargetSettings
    buildSettings.arch = settings.goArch
    buildSettings.os = settings.goOS
    buildSettings.customFlags = settings.goTags.split(' ').toTypedArray()
    goSettings.buildTargetSettings = buildSettings
}
