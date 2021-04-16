package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateSettingsPanel
import java.io.File
import javax.swing.JComponent

class SettingsWithHistory(val settings: TinyGoConfiguration) : TinyGoConfiguration by settings {
    constructor(project: Project) : this(TinyGoConfiguration.getInstance(project).deepCopy())

    override var sdk: TinyGoSdk
        get() = settings.sdk
        set(value) {
            if (value != settings.sdk) {
                settings.sdk = value
                predefinedTargets = tinygoTargets(value).toSet()
            }
        }
    override var targetPlatform: String
        get() = settings.targetPlatform
        set(value) {
            if (isTargetValid(value) && !isTargetKnown(value)) {
                settings.userTargets = userTargets + value
            }
            settings.targetPlatform = value
        }
    override var userTargets: List<String>
        get() = settings.userTargets + predefinedTargets
        set(value) {
            settings.userTargets = value
        }
    var predefinedTargets: Set<String> = tinygoTargets(settings.sdk).toSet()
    private fun isTargetValid(target: String): Boolean {
        if (predefinedTargets.contains(target)) return true
        val jsonTarget = File(target)
        return jsonTarget.exists() && jsonTarget.isFile
    }

    private fun isTargetKnown(target: String): Boolean {
        return predefinedTargets.contains(target) || settings.userTargets.contains(target)
    }
}

class TinyGoSettingsService(private val project: Project) :
    NamedConfigurable<TinyGoConfiguration>(), ConfigurationProvider<TinyGoConfiguration> {
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoSettingsService::class.java)
    }

    // local copy of the settings
    override var tinyGoSettings: TinyGoConfiguration = SettingsWithHistory(project)

    private val infoExtractor = TinyGoInfoExtractor(project)
    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun isModified(): Boolean = tinyGoSettings.modified(project)

    override fun apply() {
        logger.warn("Apply called")
        tinyGoSettings.saveState(project)
    }

    override fun getDisplayName(): String = "TinyGo"

    fun callExtractor() {
        infoExtractor.extractTinyGoInfo(tinyGoSettings) { _, output ->
            logger.trace(output)
            tinyGoSettings.extractTinyGoInfo(output)
            // update all ui fields
            propertiesWrapper.reset()
        }
    }

    override fun setDisplayName(name: String?) {
        logger.warn("Request to change display name to: $name")
    }

    override fun reset() {
        tinyGoSettings = SettingsWithHistory(project)
        propertiesWrapper.reset()
        super.reset()
    }

    override fun getEditableObject(): TinyGoConfiguration = tinyGoSettings

    override fun getBannerSlogan(): String = "Tinygo slogan"

    fun propagateGoFlags() {
        propagateGoFlags(project, tinyGoSettings)
    }

    override fun createOptionsPanel(): JComponent = generateSettingsPanel(
        propertiesWrapper,
        onDetect = this::callExtractor,
        onPropagateGoTags = this::propagateGoFlags
    )
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
