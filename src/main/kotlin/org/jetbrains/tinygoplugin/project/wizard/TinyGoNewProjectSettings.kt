package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.GoSdk
import com.goide.wizard.GoNewProjectSettings
import org.jetbrains.tinygoplugin.configuration.ITinyGoConfiguration

data class TinyGoNewProjectSettings(
    var sdk: GoSdk,
    var tinyGoSettings: ITinyGoConfiguration
) : GoNewProjectSettings(sdk)
