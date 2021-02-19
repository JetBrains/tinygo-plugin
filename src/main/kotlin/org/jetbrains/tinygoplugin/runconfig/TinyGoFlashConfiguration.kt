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
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import java.io.File
import java.nio.file.Paths

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
        return goExecutor
    }
}

class TinyGoFlashConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    GoRunConfigurationBase<TinyGoRunningState>(name, GoModuleBasedConfiguration(project), factory) {
    companion object {
        const val MAIN_FILE = "MAIN_FILE"
        const val CMD_OPTIONS = "CMD_OPTIONS"
    }

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

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeCustomField(element, MAIN_FILE, mainFile.toNioPath().toAbsolutePath().toString())
        JDOMExternalizerUtil.writeCustomField(element, CMD_OPTIONS, cmdlineOptions.joinToString(" "))
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val filePath = JDOMExternalizerUtil.readCustomField(element, MAIN_FILE)
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                val vfsFile = VfsUtil.findFile(file.toPath(), false)
                if (vfsFile != null) {
                    mainFile = vfsFile
                }
            }
        }

        JDOMExternalizerUtil.writeCustomField(element, CMD_OPTIONS, cmdlineOptions.joinToString(" "))
    }
}
