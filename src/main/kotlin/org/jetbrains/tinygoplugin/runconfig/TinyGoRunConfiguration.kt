package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoFileType
import com.goide.execution.GoModuleBasedConfiguration
import com.goide.execution.GoRunConfigurationBase
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
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.absoluteOrRelativePath
import org.jetbrains.tinygoplugin.configuration.toRelativePath
import org.jetbrains.tinygoplugin.sdk.nullSdk
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import java.io.File

private const val ERROR_SDK_NOT_SET = "run.configuration.errors.sdk"
private const val ERROR_MAIN_FILE_NOT_FOUND = "run.configuration.errors.main"
private const val ERROR_NOT_GO_FILE = "run.configuration.errors.type"
private const val CONFIGURATION_EDITOR_NAME = "run.configuration.editor.name"

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

    val executable: VirtualFile? = TinyGoConfiguration.getInstance(project).sdk.executable

    init {
        val tinyGoSettings = ConfigurationWithHistory(project)
        val projectFile = project.workspaceFile
        val workspaceFolder = projectFile?.parent?.parent
        val mainPath = workspaceFolder?.canonicalPath ?: ""
        runConfig =
            RunSettings(tinyGoSettings, "", mainPath)
        if (tinyGoSettings.sdk != nullSdk) {
            if (runConfig.gc == GarbageCollector.AUTO_DETECT || runConfig.scheduler == Scheduler.AUTO_DETECT) {
                TinyGoInfoExtractor(project).extractTinyGoInfo(runConfig) { _: GoExecutor.ExecutionResult?, output: String ->
                    runConfig.extractTinyGoInfo(output)
                    cmdlineOptions = tinyGoSettings.assembleCommandLineArguments()
                }
            } else {
                cmdlineOptions = tinyGoSettings.assembleCommandLineArguments()
            }
        }
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
        if (executable == null) {
            throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_SDK_NOT_SET))
        }
        val main = mainFile() ?: throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_MAIN_FILE_NOT_FOUND))
        if (!main.isValid) {
            throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_MAIN_FILE_NOT_FOUND))
        }
        if (!main.isDirectory && runConfig.buildType == BuildType.FOLDER) {
            throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_NOT_GO_FILE))
        }
        val mainFiletype = main.fileType
        if (mainFiletype !is GoFileType && runConfig.buildType == BuildType.FILE) {
            throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_NOT_GO_FILE))
        }
    }

    override fun createSettingsEditorGroup(): SettingsEditorGroup<TinyGoRunConfiguration> {
        val result = SettingsEditorGroup<TinyGoRunConfiguration>()
        val editor: SettingsEditor<TinyGoRunConfiguration> = TinyGoRunConfigurationEditor(this)
        result.addEditor(TinyGoBundle.message(CONFIGURATION_EDITOR_NAME, command), editor)
        return result
    }

    override fun newRunningState(environment: ExecutionEnvironment, module: Module): TinyGoRunningState {
        return TinyGoRunningState(environment, module, this)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeCustomField(element, MAIN_FILE, toRelativePath(runConfig.mainFile, project))
        JDOMExternalizerUtil.writeCustomField(element, CMD_OPTIONS, runConfig.cmdlineOptions)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val filePath = JDOMExternalizerUtil.readCustomField(element, MAIN_FILE) ?: ""
        runConfig.mainFile =
            absoluteOrRelativePath(filePath, project)?.path ?: project.basePath ?: ""
        val arguments = JDOMExternalizerUtil.readCustomField(element, CMD_OPTIONS)
        runConfig.cmdlineOptions = arguments ?: ""
        if (arguments == null && runConfig.sdk != nullSdk) cmdlineOptions = runConfig.assembleCommandLineArguments()
    }
}
