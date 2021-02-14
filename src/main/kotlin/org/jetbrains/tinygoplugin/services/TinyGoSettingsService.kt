package org.jetbrains.tinygoplugin.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UI.PanelFactory.panel
import org.jetbrains.tinygoplugin.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1

internal interface Resetable {
    fun reset()
}

class TinyGoSettingsService(private val project: Project) : NamedConfigurable<TinyGoConfiguration>(), ActionListener {
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
    private val goos = MappedGraphProperty(
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

    override fun actionPerformed(e: ActionEvent?) {
    }

    override fun setDisplayName(name: String?) {
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
            textField(property = target)
        }
        row("Compiler parameters:") {
            comboBox(EnumComboBoxModel(GarbageCollector::class.java), gc)
            comboBox(EnumComboBoxModel(Scheduler::class.java), scheduler)
        }
        row {
            button("Detect", this@TinyGoSettingsService::actionPerformed)
        }
        row("GOOS") {
            textField(property = goos).enabled(false)
        }
        row("GOARCH") {
            textField(property = goArch).enabled(false)
        }
        row("Go tags") {
            textField(property = goTags).enabled(false)
        }
    }
}
