package org.jetbrains.tinygoplugin.configuration

import com.goide.GoLibrariesUtil
import com.goide.project.GoBuildTargetSettings
import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.util.GoUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.util.application
import com.intellij.util.messages.MessageBus
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.services.propagateGoFlags
import java.util.EventListener

internal class CachedGoRootUpdater : GoModuleSettings.BuildTargetListener {
    companion object {
        val logger = logger<CachedGoRootUpdater>()
    }

    override fun changed(module: Module, batchUpdate: Boolean) {
        logger.debug("cached GOROOT update signal caught")

        val project = module.project
        val settings = TinyGoConfiguration.getInstance(project)
        if (!settings.enabled) return

        val tinyGoSettings: TinyGoConfiguration = ConfigurationWithHistory(project)
        project.service<TinyGoInfoExtractor>()
            .extractTinyGoInfo(tinyGoSettings, CachedGoRootInvalidator(project)) { _, output ->
                tinyGoSettings.extractTinyGoInfo(output)
                tinyGoSettings.saveState(project)

                propagateGoFlags(project, tinyGoSettings)
                updateExtLibrariesAndCleanCache(project)
            }

        logger.debug("cached GOROOT update signal processed")
    }
}

interface TinyGoExtractionFailureListener : EventListener {
    fun onExtractionFailure()
}

class CachedGoRootInvalidator(private val project: Project) : TinyGoExtractionFailureListener {
    override fun onExtractionFailure() {
        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        tinyGoSettings.cachedGoRoot = GoSdk.NULL
        tinyGoSettings.saveState(project)
        updateExtLibrariesAndCleanCache(project)
    }
}

private fun updateExtLibrariesAndCleanCache(project: Project) {
    if (!project.isDisposed) {
        application.assertIsDispatchThread()
        project.service<GoSdkService>().incModificationCount()
        GoUtil.cleanResolveCache(project)
        GoLibrariesUtil.updateLibraries(project, { }, null)
    }
}

fun sendReloadLibrariesSignal(project: Project) {
    if (!project.isDisposed) {
        val messageBus: MessageBus = project.messageBus
        val modules = ModuleManager.getInstance(project).modules
        modules.filter {
            GoBuildTargetSettings.DEFAULT == GoModuleSettings.getInstance(it).buildTargetSettings.goVersion
        }.forEach { messageBus.syncPublisher(GoModuleSettings.BUILD_TARGET_TOPIC).changed(it, true) }
    }
}
