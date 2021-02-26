package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateSettingsPanel
import javax.swing.JComponent

class TinyGoSettingsService(private val project: Project) :
    NamedConfigurable<TinyGoConfiguration>(), ConfigurationProvider<TinyGoConfiguration> {
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoSettingsService::class.java)
    }

    // local copy of the settings
    override var tinyGoSettings: TinyGoConfiguration = TinyGoConfiguration.getInstance(project).deepCopy()

    private val infoExtractor = TinyGoInfoExtractor(project)
    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun isModified(): Boolean = tinyGoSettings.modified(project)

    override fun apply() {
        logger.warn("Apply called")
        tinyGoSettings.saveState(project)
    }

    override fun getDisplayName(): String = "TinyGo"

    fun callExtractor() {
        val processHistory = GoHistoryProcessListener()
        infoExtractor.extractTinyGoInfo(tinyGoSettings, processHistory) {
            val output = processHistory.output.joinToString("")
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
        tinyGoSettings = TinyGoConfiguration.getInstance(project).deepCopy()
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
        fileChosen = { it.canonicalPath ?: tinyGoSettings.tinyGoSDKPath },
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
