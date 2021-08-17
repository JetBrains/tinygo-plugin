package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoRunningState
import com.goide.util.GoExecutor
import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import org.codehaus.plexus.util.cli.CommandLineUtils.translateCommandline
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured

open class TinyGoRunningState(env: ExecutionEnvironment, module: Module, configuration: TinyGoRunConfiguration) :
    GoRunningState<TinyGoRunConfiguration>(env, module, configuration) {
    // override the function to supply GoExecutor with tinygo
    protected open val hardwareArguments: List<String> = configuration.cmdlineOptions + listOf("-size", "full")

    override fun createRunExecutor(): GoExecutor {
        val arguments = listOf(configuration.command) + hardwareArguments + listOf(configuration.runConfig.mainFile)
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

class TinyGoTestRunningState(env: ExecutionEnvironment, module: Module, configuration: TinyGoRunConfiguration) :
    TinyGoRunningState(env, module, configuration) {
    override val hardwareArguments: List<String> = emptyList()
}

data class RunSettings(
    val tinyGoConfiguration: TinyGoConfiguration,
    var userArguments: String,
    var mainFile: String,
) : TinyGoConfiguration by tinyGoConfiguration {
    override fun deepCopy(): RunSettings = RunSettings(
        tinyGoConfiguration.deepCopy(),
        userArguments,
        mainFile
    )

    val cmdlineOptions: List<String>
        get() {
            val userDefined = translateCommandline(userArguments)
            return listOf("-target", targetPlatform) +
                    scheduler(userDefined, scheduler) +
                    garbageCollector(userDefined, gc) +
                    filteredUserArguments(userDefined)
        }
}

private const val GARBAGE_COLLECTOR_FLAG = "-gc"
private const val SCHEDULER_FLAG = "-scheduler"

fun garbageCollector(userArguments: Array<String>, default: GarbageCollector): Collection<String> {
    val userDefined = userArguments.indexOf(GARBAGE_COLLECTOR_FLAG)
    if (userDefined == -1 && default == GarbageCollector.AUTO_DETECT) {
        return emptyList()
    }
    if (userDefined == -1 || userDefined == userArguments.size) {
        return listOf(GARBAGE_COLLECTOR_FLAG, default.cmd)
    }
    return listOf(userArguments[userDefined], userArguments[userDefined + 1])
}

fun scheduler(userArguments: Array<String>, default: Scheduler): Collection<String> {
    val userDefined = userArguments.indexOf(SCHEDULER_FLAG)
    if (userDefined == -1 && default == Scheduler.AUTO_DETECT) {
        return emptyList()
    }
    if (userDefined == -1 || userDefined == userArguments.size) {
        return listOf(SCHEDULER_FLAG, default.cmd)
    }
    return listOf(userArguments[userDefined], userArguments[userDefined + 1])
}

fun filteredUserArguments(userArguments: Array<String>): Collection<String> {
    val result = mutableListOf<String>()
    var i = 0
    while (i < userArguments.size) {
        val arg = userArguments[i]
        if (arg == GARBAGE_COLLECTOR_FLAG || arg == SCHEDULER_FLAG) {
            i++
        } else {
            result.add(arg)
        }
        i++
    }
    return result
}
