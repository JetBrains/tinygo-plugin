package org.jetbrains.tinygoplugin.project.wizard

import com.goide.sdk.GoSdk
import com.goide.wizard.GoNewProjectSettings
import org.jetbrains.annotations.NotNull

data class TinyGoNewProjectSettings(
    var sdk: @NotNull GoSdk,
    var tinyGoSdkPath: String
) : GoNewProjectSettings(sdk)
