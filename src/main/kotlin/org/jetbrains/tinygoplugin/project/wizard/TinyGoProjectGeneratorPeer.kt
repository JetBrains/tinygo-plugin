package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.combobox.GoSdkChooserCombo
import com.goide.wizard.GoProjectGeneratorPeer
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.UI.PanelFactory
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkUtil
import org.jetbrains.tinygoplugin.ui.TinyGoUIComponents
import javax.swing.BoxLayout
import javax.swing.JPanel

class TinyGoProjectGeneratorPeer : GoProjectGeneratorPeer<TinyGoNewProjectSettings>() {
    object TinyGoInfoArgumentsImpl : TinyGoInfoArguments {
        override var tinyGoSdkPath: String = TinyGoSdkUtil.suggestSdkDirectoryStr()
        override var tinyGoTarget: String = ""
        override var tinyGoGarbageCollector: GarbageCollector = GarbageCollector.AUTO_DETECT
        override var tinyGoScheduler: Scheduler = Scheduler.AUTO_DETECT
    }

    private val propertyGraph = PropertyGraph()
    var tinyGoSdkPathProp: GraphProperty<String> =
        propertyGraph.graphProperty(TinyGoInfoArgumentsImpl::tinyGoSdkPath)
    private var targetProp: GraphProperty<String> =
        propertyGraph.graphProperty(TinyGoInfoArgumentsImpl::tinyGoTarget)
    private var gcProp: GraphProperty<GarbageCollector> =
        propertyGraph.graphProperty(TinyGoInfoArgumentsImpl::tinyGoGarbageCollector)
    private var schedulerProp: GraphProperty<Scheduler> =
        propertyGraph.graphProperty(TinyGoInfoArgumentsImpl::tinyGoScheduler)

    private fun decorateSettingsPanelForUI(component: JPanel): JPanel =
        PanelFactory.grid().add(PanelFactory.panel(component)).resize().createPanel()

    override fun createSettingsPanel(
        parentDisposable: Disposable,
        locationComponent: LabeledComponent<TextFieldWithBrowseButton>?,
        sdkCombo: GoSdkChooserCombo?,
        project: Project?
    ): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
        panel.add(createGridPanel(locationComponent, sdkCombo).resize().createPanel())
        panel.add(
            decorateSettingsPanelForUI(
                TinyGoUIComponents.generateTinyGoParametersPanel(
                    tinyGoSdkPathProp,
                    {
                        if (TinyGoSdkUtil.checkDirectoryForTinyGo(it)) it.canonicalPath!!
                        else {
                            Messages.showErrorDialog("Selected TinyGo path is invalid", "Invalid TinyGo")
                            TinyGoSdkUtil.suggestSdkDirectoryStr()
                        }
                    },
                    targetProp,
                    gcProp,
                    schedulerProp,
                )
            )
        )
        return panel
    }

    override fun getSettings(): TinyGoNewProjectSettings {
        return TinyGoNewProjectSettings(
            sdk = sdkFromCombo,
            tinyGoSdkPath = tinyGoSdkPathProp.get(),
            tinyGoTarget = targetProp.get(),
            tinyGoGarbageCollector = gcProp.get(),
            tinyGoScheduler = schedulerProp.get()
        )
    }
}
