package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoFileType
import com.goide.execution.GoConsoleFilter
import com.goide.execution.GoRunUtil
import com.goide.execution.GoRuntimeErrorsListener
import com.goide.util.GoExecutor
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironmentAwareRunProfileState
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.attach
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import com.intellij.util.io.BaseDataReader.SleepingPolicy
import com.intellij.util.io.BaseOutputReader
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

class TinyGoFlashConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<TinyGoFlashConfiguration>(project, factory, name) {
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

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState {
        val tinyGoExecutablePath = Paths.get(
            Paths.get(tinyGoSDKPath).toAbsolutePath().toString(),
            "bin",
            "tinygo"
        )
        val arguments = cmdlineOptions + listOf(mainFile.path)
        val goExecutor = GoExecutor.`in`(project, null)
        goExecutor.withExePath(tinyGoExecutablePath.toString())
        goExecutor.withParameters(arguments)
        val cmdBuilder = goExecutor.createTargetedCommandLine()
        val request = cmdBuilder.request
        val cmd = cmdBuilder.build()
        val environmentFactory = goExecutor.targetEnvironmentFactory
        val environment = environmentFactory.prepareRemoteEnvironment(
            request,
            TargetEnvironmentAwareRunProfileState.TargetProgressIndicator.EMPTY
        )
        val commandRepresentation: String = cmd.getCommandPresentation(environment)
        val process = environment.createProcess(cmd, EmptyProgressIndicator())
        val environmentVariables: Map<String, String> = cmd.environmentVariables
        /*
      for pty we need to use TerminalExecutionConsole to otherwise stdin will be duplicated.
      TerminalExecutionConsole doesn't support colored process handlers {@link TerminalExecutionConsole.isAcceptable(processHandler)},
      and implements coloring by itself.
      - we need to use non-colored process handler
      - we need to disable splitting line to avoid GO-7042
       */
        val handler = object : KillableProcessHandler(process, commandRepresentation, cmd.getCharset()) {
            override fun startNotify() {
                if (goExecutor.isShowGoEnvVariables) {
                    GoRunUtil.printGoEnvVariables(environmentVariables, this)
                }
                super.startNotify()
            }

            override fun readerOptions(): BaseOutputReader.Options {
                val options = BaseOutputReader.Options.forMostlySilentProcess()
                return object : BaseOutputReader.Options() {
                    override fun policy(): SleepingPolicy {
                        return options.policy()
                    }

                    override fun splitToLines(): Boolean {
                        return false
                    }
                }
            }
        }
        handler.setShouldKillProcessSoftlyWithWinP(true)
        ProcessTerminatedListener.attach(
            handler, project,
            """
            
            TinyGo execution finished with exit code 1
            
            """.trimIndent()
        )
        val errorListener = GoRuntimeErrorsListener(project, true)
        handler.addProcessListener(errorListener)
        val processHandler = handler
        val console = createConsole(processHandler, executionEnvironment)
        console.addMessageFilter(GoConsoleFilter(project, null, project.workspaceFile!!.url))
        console.attachToProcess(processHandler)
        return RunProfileState { _, _ -> DefaultExecutionResult(console, processHandler) }
    }

    @Throws(ExecutionException::class)
    private fun createConsole(
        processHandler: ProcessHandler,
        environment: ExecutionEnvironment,
    ): ConsoleView {
        val project: Project = project
        if (TerminalExecutionConsole.isAcceptable(processHandler)) {
            return TerminalExecutionConsole(project, processHandler).withEnterKeyDefaultCodeEnabled(true)
        }
        val searchScope = GlobalSearchScopes.executionScope(project, environment.getRunProfile())
        return ConsoleViewImpl(project, searchScope, false, false)
    }

    override fun getConfigurationEditor(): SettingsEditor<out TinyGoFlashConfiguration> {
        return TinyGoConfigurationEditor(this)
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
}
