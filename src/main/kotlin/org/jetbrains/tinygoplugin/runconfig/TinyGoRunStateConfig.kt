package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoRunningState
import com.goide.util.GoExecutor
import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured

class TinyGoRunningState(env: ExecutionEnvironment, module: Module, configuration: TinyGoRunConfiguration) :
    GoRunningState<TinyGoRunConfiguration>(env, module, configuration) {
    // override the function to supply GoExecutor with tinygo
    override fun createRunExecutor(): GoExecutor {
        val arguments =
            listOf(configuration.command) +
                configuration.cmdlineOptions +
                listOf(configuration.runConfig.mainFile)
        val tinyGoExecutablePath = configuration.executable
        if (tinyGoExecutablePath == null) {
            notifyTinyGoNotConfigured(configuration.project, "TinyGo SDK is not set. Please configure TinyGo SDK")
            throw ExecutionException("TinyGo SDK is not set. Please configure TinyGo SDK")
        }

        return GoExecutor.`in`(configuration.project, null)
            .withExePath(tinyGoExecutablePath.path)
            .withParameters(arguments)
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
