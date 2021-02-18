package org.jetbrains.tinygoplugin.services

import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.CanResetSettingsUI
import org.jetbrains.tinygoplugin.ui.ResetableProperty
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.TinyGoUIComponents
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JComponent

class TinyGoSettingsService(private val project: Project) : NamedConfigurable<TinyGoConfiguration>(),
    CanResetSettingsUI, ActionListener {
    companion object {
        private val logger: Logger = Logger.getInstance(TinyGoSettingsService::class.java)
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

    private fun callExtractor() {
        val processHistory = GoHistoryProcessListener()
        infoExtractor.extractTinyGoInfo(settings, processHistory) { result ->
            val output = processHistory.output.joinToString("")
            logger.trace(output)
            settings.extractTinyGoInfo(output)
            // update all ui fields
            propertiesWrapper.goArch.reset()
            propertiesWrapper.goTags.reset()
            propertiesWrapper.goOS.reset()

            propertiesWrapper.gc.reset()
            propertiesWrapper.scheduler.reset()
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        callExtractor()
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

    override fun createOptionsPanel(): JComponent = TinyGoUIComponents.generateSettingsPanel(
        propertiesWrapper,
        fileChosen = { it.canonicalPath ?: settings.tinyGoSDKPath },
        this::actionPerformed,
        project
    )
}
