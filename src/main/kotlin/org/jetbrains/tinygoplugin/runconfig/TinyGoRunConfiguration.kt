package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoFileType
import com.goide.execution.GoModuleBasedConfiguration
import com.goide.execution.GoRunConfigurationBase
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

class TinyGoRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
    runType: TinyGoCommandType,
) :
    GoRunConfigurationBase<TinyGoRunningState>(name, GoModuleBasedConfiguration(project), factory),
    ConfigurationProvider<RunSettings> {
    companion object {
        const val MAIN_FILE = "tinygo_main_file"
        const val CMD_OPTIONS = "tinygo_cmd_options"
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
        val projectFile = project.workspaceFile
        val workspaceFolder = projectFile?.parent?.parent
        val mainPath = workspaceFolder?.canonicalPath ?: ""
        runConfig =
            RunSettings(tinyGoSettings, "", mainPath)
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

    override fun createSettingsEditorGroup(): SettingsEditorGroup<TinyGoRunConfiguration> {
        val result = SettingsEditorGroup<TinyGoRunConfiguration>()
        val editor: SettingsEditor<TinyGoRunConfiguration> = TinyGoRunConfigurationEditor(this)
        result.addEditor("TinyGo $command", editor)
        return result
    }

    override fun newRunningState(environment: ExecutionEnvironment, module: Module): TinyGoRunningState {
        return TinyGoRunningState(environment, module, this)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeCustomField(element, MAIN_FILE, runConfig.mainFile)
        JDOMExternalizerUtil.writeCustomField(element, CMD_OPTIONS, runConfig.cmdlineOptions)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val filePath = JDOMExternalizerUtil.readCustomField(element, MAIN_FILE)
        runConfig.mainFile =
            (filePath ?: project.workspaceFile!!.parent.parent.canonicalPath.toString())
        val arguments = JDOMExternalizerUtil.readCustomField(element, CMD_OPTIONS)
        runConfig.cmdlineOptions = arguments ?: ""
        if (arguments == null) cmdlineOptions = runConfig.assembleCommandLineArguments()
    }
}
