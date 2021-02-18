package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoFileType
import com.goide.execution.GoModuleBasedConfiguration
import com.goide.execution.GoRunConfigurationBase
import com.goide.execution.GoRunningState
import com.goide.util.GoExecutor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import java.nio.file.Path
import java.nio.file.Paths
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
        tinyGoFlashConfiguration.mainFile = VfsUtil.findFile(Path.of(main.get()), true)!!
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

class TinyGoRunningState(env: ExecutionEnvironment, module: Module, configuration: TinyGoFlashConfiguration) :
    GoRunningState<TinyGoFlashConfiguration>(env, module, configuration) {
    // override the function to supply GoExecutor with tinygo
    override fun createRunExecutor(): GoExecutor {
        val arguments = configuration.cmdlineOptions + listOf(configuration.mainFile.path)
        val goExecutor = GoExecutor.`in`(configuration.project, null)
        val tinyGoExecutablePath = Paths.get(
            Paths.get(configuration.tinyGoSDKPath).toAbsolutePath().toString(),
            "bin",
            "tinygo"
        )

        goExecutor.withExePath(tinyGoExecutablePath.toString())
        goExecutor.withParameters(arguments)
        return super.createRunExecutor()
    }
}

class TinyGoFlashConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    GoRunConfigurationBase<TinyGoRunningState>(name, GoModuleBasedConfiguration(project), factory) {
    var tinyGoSDKPath: String = ""
    var target = ""
    var gc = GarbageCollector.AUTO_DETECT
    var scheduler = Scheduler.AUTO_DETECT
    var mainFile: VirtualFile = project.workspaceFile!!.parent.parent
    var cmdlineOptions: MutableCollection<String> = ArrayList()

    init {
        val settings = TinyGoConfiguration.getInstance(project)
        tinyGoSDKPath = settings.tinyGoSDKPath
        target = settings.targetPlatform
        gc = settings.gc
        scheduler = settings.scheduler
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        if (!mainFile.isValid) {
            throw RuntimeConfigurationException("Main file does not exists")
        }
        val mainFiletype = mainFile.fileType
        if (mainFiletype !is GoFileType) {
            throw RuntimeConfigurationException("Selected file is not a go file")
        }
    }

    override fun createSettingsEditorGroup(): SettingsEditorGroup<TinyGoFlashConfiguration> {
        val result = SettingsEditorGroup<TinyGoFlashConfiguration>()
        val editor: SettingsEditor<TinyGoFlashConfiguration> = TinyGoConfigurationEditor(this)
        result.addEditor("TinyGoEditorName", editor)
        return result
    }

    override fun newRunningState(p0: ExecutionEnvironment, p1: Module): TinyGoRunningState {
        return TinyGoRunningState(p0, p1, this)
    }
}
