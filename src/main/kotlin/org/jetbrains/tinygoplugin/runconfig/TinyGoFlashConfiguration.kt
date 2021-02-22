package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoFileType
import com.goide.execution.GoModuleBasedConfiguration
import com.goide.execution.GoRunConfigurationBase
import com.goide.execution.GoRunningState
import com.goide.util.GoExecutor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import java.io.File
import java.nio.file.Paths

class TinyGoRunningState(env: ExecutionEnvironment, module: Module, configuration: TinyGoFlashConfiguration) :
    GoRunningState<TinyGoFlashConfiguration>(env, module, configuration) {
    // override the function to supply GoExecutor with tinygo
    override fun createRunExecutor(): GoExecutor {
        val arguments =
            listOf(configuration.command) +
                configuration.cmdlineOptions +
                listOf(configuration.runConfig.mainFile)
        val goExecutor = GoExecutor.`in`(configuration.project, null)
        val tinyGoExecutablePath = Paths.get(
            Paths.get(configuration.tinyGoSettings.tinyGoSDKPath).toAbsolutePath().toString(),
            "bin",
            "tinygo"
        )

        goExecutor.withExePath(tinyGoExecutablePath.toString())
        goExecutor.withParameters(arguments)
        return goExecutor
    }
}

data class RunSettings(
    val tinyGoConfiguration: TinyGoConfiguration,
    var cmdlineOptions: String,
    var mainFile: String,
) : TinyGoConfiguration by tinyGoConfiguration {
    override fun deepCopy(): RunSettings = RunSettings(
        tinyGoConfiguration.deepCopy(),
        cmdlineOptions,
        mainFile
    )
}

fun TinyGoConfiguration.assembleCommandLineArguments(): Collection<String> {
    /* ktlint-disable */
    return listOf(
        "-target", targetPlatform,
        "-scheduler", scheduler.cmd,
        "-gc", gc.cmd
    )
    /* ktlint-enable */
}

class TinyGoFlashConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
    runType: ConfigurationType,
) :
    GoRunConfigurationBase<TinyGoRunningState>(name, GoModuleBasedConfiguration(project), factory),
    ConfigurationProvider<RunSettings> {
    companion object {
        const val MAIN_FILE = "MAIN_FILE"
        const val CMD_OPTIONS = "CMD_OPTIONS"
    }

    val command = runType.command
    var runConfig: RunSettings
    var cmdlineOptions: Collection<String>
        get() = runConfig.cmdlineOptions.split(' ').map(String::trim).filterNot(String::isEmpty)
        set(value) {
            runConfig.cmdlineOptions = value.joinToString(" ")
        }

    override val tinyGoSettings: RunSettings
        get() = runConfig

    init {
        val tinyGoSettings = TinyGoConfiguration.getInstance(project).deepCopy()
        val main = project.workspaceFile!!.parent.parent
        runConfig =
            RunSettings(tinyGoSettings, "", main.canonicalPath!!)
        cmdlineOptions = tinyGoSettings.assembleCommandLineArguments()
    }

    private fun mainFile(): VirtualFile? {
        if (runConfig.mainFile.isEmpty()) {
            return null
        }
        val file = File(runConfig.mainFile)
        if (!file.exists()) {
            return null
        }
        return VfsUtil.findFile(file.toPath(), false)
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        val main = mainFile() ?: throw RuntimeConfigurationException("Main file does not exists")
        if (!main.isValid) {
            throw RuntimeConfigurationException("Main file does not exists")
        }
        val mainFiletype = main.fileType
        if (mainFiletype !is GoFileType) {
            throw RuntimeConfigurationException("Selected file is not a go file")
        }
    }

    override fun createSettingsEditorGroup(): SettingsEditorGroup<TinyGoFlashConfiguration> {
        val result = SettingsEditorGroup<TinyGoFlashConfiguration>()
        val editor: SettingsEditor<TinyGoFlashConfiguration> = TinyGoRunConfigurationEditor(this)
        result.addEditor("TinyGoEditorName", editor)
        return result
    }

    override fun newRunningState(p0: ExecutionEnvironment, p1: Module): TinyGoRunningState {
        return TinyGoRunningState(p0, p1, this)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeCustomField(element, MAIN_FILE, runConfig.mainFile)
        JDOMExternalizerUtil.writeCustomField(element, CMD_OPTIONS, runConfig.cmdlineOptions)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val filePath = JDOMExternalizerUtil.readCustomField(element, MAIN_FILE)
        if (filePath != null) {
            runConfig.mainFile = filePath
        } else {
            runConfig.mainFile = project.workspaceFile!!.parent.parent.canonicalPath.toString()
        }
        val arguments = JDOMExternalizerUtil.readCustomField(element, CMD_OPTIONS)
        if (arguments != null) {
            runConfig.cmdlineOptions = arguments
        } else {
            cmdlineOptions = runConfig.assembleCommandLineArguments()
        }
    }
}
