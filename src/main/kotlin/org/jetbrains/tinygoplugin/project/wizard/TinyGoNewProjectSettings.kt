package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.GoSdk
import com.goide.wizard.GoNewProjectSettings
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler

interface TinyGoInfoArguments {
    var tinyGoSdkPath: String
    var tinyGoTarget: String
    var tinyGoGarbageCollector: GarbageCollector
    var tinyGoScheduler: Scheduler
}

data class TinyGoNewProjectSettings(
    var sdk: GoSdk,
    override var tinyGoSdkPath: String,
    override var tinyGoTarget: String,
    override var tinyGoGarbageCollector: GarbageCollector,
    override var tinyGoScheduler: Scheduler,
) : GoNewProjectSettings(sdk), TinyGoInfoArguments
