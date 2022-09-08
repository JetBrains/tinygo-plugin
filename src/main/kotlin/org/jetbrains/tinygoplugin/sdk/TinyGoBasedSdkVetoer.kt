package org.jetbrains.tinygoplugin.sdk

import com.goide.sdk.GoBasedSdk
import com.goide.sdk.GoBasedSdkVetoer
import com.goide.sdk.GoSdkService
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration

class TinyGoBasedSdkVetoer : GoBasedSdkVetoer {
    override fun isSdkVetoed(sdk: GoBasedSdk, module: Module): Boolean =
        module.project.tinyGoConfiguration().enabled &&
            module.project.service<GoSdkService>().getSdk(module) == sdk
}
