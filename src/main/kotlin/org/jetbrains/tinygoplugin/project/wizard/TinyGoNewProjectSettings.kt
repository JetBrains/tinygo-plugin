package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.GoSdk
import com.goide.wizard.GoNewProjectSettings
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

data class TinyGoNewProjectSettings(
    var goSdk: GoSdk,
    var tinyGoSettings: TinyGoConfiguration,
) : GoNewProjectSettings(goSdk)