package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.CanResetSettingsUI
import org.jetbrains.tinygoplugin.ui.ResetableProperty
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateSettingsPanel
import javax.swing.JComponent

class TinyGoSettingsService(private val project: Project) : NamedConfigurable<TinyGoConfiguration>(),
    CanResetSettingsUI {
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoSettingsService::class.java)
    }

    // local copy of the settings
    override var settings: TinyGoConfiguration = TinyGoConfiguration.getInstance(project).deepCopy()

    // list of all UI properties to be resetted
    override var resetableProperties: MutableCollection<ResetableProperty> = ArrayList()

    private val infoExtractor = TinyGoInfoExtractor(project)
    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun isModified(): Boolean = settings.modified(project)

    override fun apply() {
        logger.warn("Apply called")
        settings.saveState(project)
    }

    override fun getDisplayName(): String = "TinyGo"

    fun callExtractor() {
        val processHistory = GoHistoryProcessListener()
        infoExtractor.extractTinyGoInfo(settings, processHistory) {
            val output = processHistory.output.joinToString("")
            logger.trace(output)
            settings.extractTinyGoInfo(output)
            // update all ui fields
            resetableProperties.forEach(ResetableProperty::reset)
        }
    }

    override fun setDisplayName(name: String?) {
        logger.warn("Request to change display name to: $name")
    }

    override fun reset() {
        settings = TinyGoConfiguration.getInstance(project).deepCopy()
        resetableProperties.forEach(ResetableProperty::reset)
        super.reset()
    }

    override fun getEditableObject(): TinyGoConfiguration = settings

    override fun getBannerSlogan(): String = "Tinygo slogan"

    fun propagateGoFlags() {
        propagateGoFlags(project, settings)
    }

    override fun createOptionsPanel(): JComponent = generateSettingsPanel(
        propertiesWrapper,
        fileChosen = { it.canonicalPath ?: settings.tinyGoSDKPath },
        this::callExtractor,
        this::propagateGoFlags,
        project
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
