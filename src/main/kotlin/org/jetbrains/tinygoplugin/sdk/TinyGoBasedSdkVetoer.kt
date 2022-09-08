package org.jetbrains.tinygoplugin.sdk

import com.goide.sdk.GoBasedSdk
import com.goide.sdk.GoBasedSdkVetoer
import com.goide.sdk.GoSdkService
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

class TinyGoBasedSdkVetoer : GoBasedSdkVetoer {
    override fun isSdkVetoed(sdk: GoBasedSdk, module: Module): Boolean =
        TinyGoConfiguration.getInstance(module.project).enabled &&
            module.project.service<GoSdkService>().getSdk(module) == sdk
}
