package org.jetbrains.tinygoplugin.runconfig

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import java.nio.file.Path
import javax.swing.JComponent


class TinyGoRunConfigurationEditor(private val runConfiguration: TinyGoFlashConfiguration) :
    SettingsEditor<TinyGoFlashConfiguration>() {

    private val properties = TinyGoPropertiesWrapper(runConfiguration)

    init {
        resetEditorFrom(runConfiguration)
    }

    override fun resetEditorFrom(configuration: TinyGoFlashConfiguration) {
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
