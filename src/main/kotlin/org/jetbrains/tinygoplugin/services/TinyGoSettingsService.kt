package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.goide.util.GoHistoryProcessListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1

internal interface Resetable {
    fun reset()
}

class TinyGoSettingsService(private val project: Project) : NamedConfigurable<TinyGoConfiguration>() {
    companion object {
        private val logger: Logger = Logger.getInstance(TinyGoSettingsService::class.java)
    }
    // list of all UI properties to be resetted
    private val resetableProperties: MutableCollection<Resetable> = ArrayList()
    // wrapper around graph property that binds the field to the property in settings
    inner class MappedGraphProperty<T>(
        private val prop: GraphProperty<T>,
        private val objProperty: KMutableProperty1<TinyGoConfiguration, T>,
    ) : GraphProperty<T> by prop, Resetable {
        init {
            prop.afterChange {
                objProperty.set(this@TinyGoSettingsService.settings, it)
            }
            prop.afterReset {
                prop.set(objProperty.get(this@TinyGoSettingsService.settings))
            }
            this@TinyGoSettingsService.resetableProperties.add(this)
        }

        override fun reset() = prop.reset()
    }

    private val infoExtractor = TinyGoInfoExtractor(project)

    // local copy of the settings
    private var settings = TinyGoConfiguration.getInstance(project).deepCopy()

    private val propertyGraph = PropertyGraph()
    // set initial string
    private val tinygoSDKPath =
        MappedGraphProperty(
            prop = propertyGraph.graphProperty(settings::tinyGoSDKPath),
            objProperty = TinyGoConfiguration::tinyGoSDKPath
        )
    private val target = MappedGraphProperty(
        prop = propertyGraph.graphProperty(settings::targetPlatform),
        objProperty = TinyGoConfiguration::targetPlatform
    )

    private val gc = MappedGraphProperty(
        prop = propertyGraph.graphProperty(settings::gc),
        objProperty = TinyGoConfiguration::gc
    )
    private val scheduler = MappedGraphProperty(
        prop = propertyGraph.graphProperty(settings::scheduler),
        objProperty = TinyGoConfiguration::scheduler
    )
    private val goOS = MappedGraphProperty(
        prop = propertyGraph.graphProperty(settings::goOS),
        objProperty = TinyGoConfiguration::goOS
    )
    private val goArch = MappedGraphProperty(
        prop = propertyGraph.graphProperty(settings::goArch),
        objProperty = TinyGoConfiguration::goArch
    )
    private val goTags = MappedGraphProperty(
        prop = propertyGraph.graphProperty(settings::goTags),
        objProperty = TinyGoConfiguration::goTags
    )

    override fun isModified(): Boolean = settings.modified(project)

    override fun apply() {
        logger.warn("Apply called")
        settings.saveState(project)
    }

    override fun getDisplayName(): String = "TinyGo"

    fun extractTinyGOParameters() {
        val executor = infoExtractor.assembleTinyGoShellCommand(settings)
        val processHistory = GoHistoryProcessListener()
        executor.executeWithProgress(true, true, processHistory, null) { result ->
            val output = processHistory.output.joinToString("")
            logger.trace(output)
            settings.extractTinyGoInfo(output)
            // update all ui fields
            resetableProperties.forEach(Resetable::reset)
        }
    }

    override fun setDisplayName(name: String?) {
        logger.warn("Request to change display name to: $name")
    }

    override fun reset() {
        settings = TinyGoConfiguration.getInstance(project).deepCopy()
        resetableProperties.forEach(Resetable::reset)
        super.reset()
    }

    override fun getEditableObject(): TinyGoConfiguration = settings

    override fun getBannerSlogan(): String = "Tinygo slogan"

    override fun createOptionsPanel(): JComponent = panel {
        row("TinyGo Path") {
            textFieldWithBrowseButton(
                property = tinygoSDKPath, project = project,
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                fileChosen = { it.canonicalPath ?: settings.tinyGoSDKPath }
            )
        }
        row("Target platform") {
            textField(property = target).growPolicy(GrowPolicy.MEDIUM_TEXT)
        }
        row("Compiler parameters:") {
            row("Garbage collector") {
                comboBox(EnumComboBoxModel(GarbageCollector::class.java), gc)
            }
            row("Scheduler") {
                comboBox(EnumComboBoxModel(Scheduler::class.java), scheduler)
            }
        }
        row {
            button("Detect") { extractTinyGOParameters() }
            button("Update gopath") { propagateGoFlags() }
        }
        row("GOOS") {
            textField(property = goOS).growPolicy(GrowPolicy.MEDIUM_TEXT)
        }
        row("GOARCH") {
            textField(property = goArch).growPolicy(GrowPolicy.MEDIUM_TEXT)
        }
        row("Go tags") {
            textField(property = goTags).growPolicy(GrowPolicy.MEDIUM_TEXT)
        }
    }

    private fun propagateGoFlags() {
        val goSettings = ModuleManager.getInstance(project).modules.map {
            it?.getService(GoModuleSettings::class.java)
        }.filterNotNull().firstOrNull()
        if (goSettings == null) {
            logger.warn("Could not find go module settings")
            return
        }
        val buildSettings = goSettings.buildTargetSettings
        buildSettings.arch = settings.goArch
        buildSettings.os = settings.goOS
        buildSettings.customFlags = settings.goTags.split(' ').toTypedArray()
        goSettings.buildTargetSettings = buildSettings
    }
}
