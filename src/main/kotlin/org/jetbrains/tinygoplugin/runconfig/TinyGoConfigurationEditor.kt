package org.jetbrains.tinygoplugin.runconfig

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import com.intellij.util.io.exists
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import java.nio.file.Path
import javax.swing.JComponent

class TinyGoConfigurationEditor(defaultConfiguration: TinyGoFlashConfiguration) :
    SettingsEditor<TinyGoFlashConfiguration>() {

    private val propertyGraph = PropertyGraph()
    private val sdkProperty = propertyGraph.graphProperty { "" }
    private val gcProperty = propertyGraph.graphProperty { GarbageCollector.AUTO_DETECT }
    private val schedulerProperty = propertyGraph.graphProperty { Scheduler.AUTO_DETECT }
    private val targetProperty = propertyGraph.graphProperty { "" }
    private val tinyGoArguments = propertyGraph.graphProperty { "" }
    private val main = propertyGraph.graphProperty { "" }

    init {
        resetEditorFrom(defaultConfiguration)
    }

    override fun resetEditorFrom(configuration: TinyGoFlashConfiguration) {
        sdkProperty.set(configuration.tinyGoSDKPath)
        sdkProperty.set(configuration.tinyGoSDKPath)
        gcProperty.set(configuration.gc)
        schedulerProperty.set(configuration.scheduler)
        targetProperty.set(configuration.target)
        main.set(configuration.mainFile.path)
        /* ktlint-disable */
        tinyGoArguments.set(
            listOf(
                "flash",
                "-target", configuration.target,
                "-scheduler", configuration.scheduler.cmd,
                "-gc", configuration.gc.cmd
            ).joinToString(separator = " ")
            /* ktlint-enable */
        )
    }

    override fun applyEditorTo(tinyGoFlashConfiguration: TinyGoFlashConfiguration) {
        tinyGoFlashConfiguration.cmdlineOptions = tinyGoArguments.get().split(' ').filter {
            it.trim().isNotEmpty()
        }.toMutableList()
        val mainFile = Path.of(main.get())
        if (mainFile.exists()) {
            tinyGoFlashConfiguration.mainFile = VfsUtil.findFile(mainFile, true)!!
        }
    }

    override fun createEditor(): JComponent {
        return panel {
            row("TinyGo path") {
                textField(sdkProperty).enabled(false)
            }
            row("Target") {
                textField(targetProperty).enabled(false)
            }
            row("Command line arguments") {
                textField(tinyGoArguments).growPolicy(GrowPolicy.MEDIUM_TEXT)
            }
            row("Path to main") {
                val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
                textFieldWithBrowseButton(
                    property = main,
                    fileChooserDescriptor = fileChooserDescriptor
                )
            }
        }
    }
}