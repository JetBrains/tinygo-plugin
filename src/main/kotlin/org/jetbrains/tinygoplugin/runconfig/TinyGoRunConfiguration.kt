package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoFileType
import com.goide.execution.GoModuleBasedConfiguration
import com.goide.execution.GoRunConfigurationBase
import com.intellij.conversion.impl.ConversionContextImpl
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
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.runconfig.TinyGoRunConfigurationEditor.PathKind
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkVersion
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

private const val ERROR_SDK_NOT_SET = "run.configuration.errors.sdk"
private const val ERROR_MAIN_FILE_NOT_FOUND = "run.configuration.errors.main"
private const val ERROR_NOT_GO_FILE = "run.configuration.errors.type"
private const val ERROR_BUILD_WASM_WRONG_EXTENSION = "run.configuration.errors.build.wasm.wrongExtension"
private const val ERROR_HEAP_ALLOCS_OUTDATED_COMPILER = "run.configuration.errors.heapAllocs.outdatedCompiler"
private const val CONFIGURATION_EDITOR_NAME = "run.configuration.editor.name"
private const val MAIN_FILE = "tinygo_main_file"
private const val CMD_OPTIONS = "tinygo_cmd_options"
private const val OUTPUT_PATH = "tinygo_output_path"

abstract class TinyGoRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
    runType: TinyGoCommandType
) :
    GoRunConfigurationBase<TinyGoRunningState>(name, GoModuleBasedConfiguration(project), factory),
    ConfigurationProvider<RunSettings> {
    val command = runType.command
    var runConfig: RunSettings
    private val context = ConversionContextImpl(Path.of(project.basePath ?: project.projectFile?.path ?: ""))

    val cmdlineOptions: List<String>
        get() = runConfig.cmdlineOptions

    override val tinyGoSettings: RunSettings
        get() = runConfig

    val executable: VirtualFile?
        get() = project.tinyGoConfiguration().sdk.executable

    init {
        val tinyGoSettings = ConfigurationWithHistory(project)
        val projectFile = project.workspaceFile
        val workspaceFolder = projectFile?.parent?.parent
        val mainPath = workspaceFolder?.canonicalPath ?: ""
        runConfig =
            RunSettings(tinyGoSettings, "", mainPath, "")
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
        val mainFiletype = main.fileType
        if (mainFiletype !is GoFileType && !main.isDirectory) {
            throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_NOT_GO_FILE))
        }
    }

    override fun newRunningState(environment: ExecutionEnvironment, module: Module): TinyGoRunningState {
        return TinyGoRunningState(environment, module, this)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeCustomField(element, MAIN_FILE, context.collapsePath(runConfig.mainFile))
        JDOMExternalizerUtil.writeCustomField(element, CMD_OPTIONS, runConfig.userArguments)
        JDOMExternalizerUtil.writeCustomField(element, MAIN_FILE, context.collapsePath(runConfig.mainFile))
        JDOMExternalizerUtil.writeCustomField(element, OUTPUT_PATH, context.collapsePath(runConfig.outputPath))
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val filePath = JDOMExternalizerUtil.readCustomField(element, MAIN_FILE) ?: ""
        runConfig.mainFile = context.expandPath(filePath)
        val arguments = JDOMExternalizerUtil.readCustomField(element, CMD_OPTIONS)
        runConfig.userArguments = arguments ?: ""
        val outputPath = JDOMExternalizerUtil.readCustomField(element, OUTPUT_PATH) ?: ""
        runConfig.outputPath = context.expandPath(outputPath)
    }
}

open class TinyGoRunConfigurationImpl(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
    runType: TinyGoCommandType,
    private val pathKind: PathKind = PathKind.MAIN
) : TinyGoRunConfiguration(project, factory, name, runType) {
    override fun createSettingsEditorGroup(): SettingsEditorGroup<TinyGoRunConfiguration> {
        val result = SettingsEditorGroup<TinyGoRunConfiguration>()
        val editor: SettingsEditor<TinyGoRunConfiguration> = TinyGoRunConfigurationEditor(this, pathKind)
        result.addEditor(TinyGoBundle.message(CONFIGURATION_EDITOR_NAME, command), editor)
        return result
    }

    override fun supportsDelve(): Boolean = false
}

class TinyGoTestRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    TinyGoRunConfigurationImpl(project, factory, name, TinyGoTestCommand, PathKind.TEST) {
    override fun newRunningState(environment: ExecutionEnvironment, module: Module): TinyGoRunningState {
        return TinyGoTestRunningState(environment, module, this)
    }
}

open class TinyGoBuildRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    TinyGoRunConfiguration(project, factory, name, TinyGoBuildCommand) {
    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        super.checkConfiguration()
        if (!isWasmTarget(project.tinyGoConfiguration().targetPlatform)) return
        val outputPath = runConfig.outputPath
        if (outputPath.isNotEmpty()) {
            val candidate = Paths.get(outputPath)
            val existingFile = candidate.exists() && candidate.isRegularFile()
            val newFile = candidate.notExists()
            if ((existingFile || newFile) && candidate.extension != "wasm")
                throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_BUILD_WASM_WRONG_EXTENSION))
        }
    }

    override fun newRunningState(environment: ExecutionEnvironment, module: Module): TinyGoRunningState {
        return TinyGoBuildRunningState(environment, module, this)
    }

    override fun createSettingsEditorGroup(): SettingsEditorGroup<TinyGoBuildRunConfiguration> {
        val result = SettingsEditorGroup<TinyGoBuildRunConfiguration>()
        val editor: SettingsEditor<TinyGoBuildRunConfiguration> = TinyGoBuildRunConfigurationEditor(this)
        result.addEditor(TinyGoBundle.message(CONFIGURATION_EDITOR_NAME, command), editor)
        return result
    }
}

class TinyGoHeapAllocRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    TinyGoRunConfigurationImpl(project, factory, name, TinyGoBuildCommand, PathKind.MAIN) {
    @Suppress("MagicNumber")
    companion object {
        private val TINYGO_PRINT_HEAP_ALLOCS_MIN_VER = TinyGoSdkVersion(0, 18, 0)
    }

    override fun newRunningState(environment: ExecutionEnvironment, module: Module): TinyGoRunningState {
        return TinyGoHeapAllocRunningState(environment, module, this)
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        super.checkConfiguration()

        val sdk = project.tinyGoConfiguration().sdk
        if (sdk.sdkVersion.isLessThan(TINYGO_PRINT_HEAP_ALLOCS_MIN_VER)) {
            throw RuntimeConfigurationException(TinyGoBundle.message(ERROR_HEAP_ALLOCS_OUTDATED_COMPILER))
        }
    }
}
