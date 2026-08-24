package org.jetbrains.tinygoplugin.configuration

import com.goide.GoLibrariesUtil
import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.util.GoUtil
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.util.application
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.messages.MessageBus
import kotlinx.coroutines.launch
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.TinyGoLibraryLayoutService
import org.jetbrains.tinygoplugin.services.TinyGoServiceScope
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.services.propagateGoFlags
import java.util.EventListener
import java.util.concurrent.atomic.AtomicReference

internal class GoSdkChangeListener(private val project: Project) : ModuleRootListener {
    private val lastGoSdkUrl = AtomicReference<String?>()

    override fun rootsChanged(event: ModuleRootEvent) {
        TinyGoServiceScope.getScope(project).launch {
            val currentGoSdkUrl = project.service<GoSdkService>().getSdk(null).homeUrl
            val previousGoSdkUrl = lastGoSdkUrl.getAndSet(currentGoSdkUrl)
            if (previousGoSdkUrl != currentGoSdkUrl && project.tinyGoConfiguration().enabled) {
                logger<GoSdkChangeListener>().debug(
                    "Go SDK changed from '$previousGoSdkUrl' to '$currentGoSdkUrl'; updating cached GOROOT"
                )
                sendReloadLibrariesSignal(project)
            }
        }
    }
}

internal class CachedGoRootUpdater : GoModuleSettings.BuildTargetListener {
    companion object {
        val logger = logger<CachedGoRootUpdater>()
    }

    override fun changed(module: Module, batchUpdate: Boolean) {
        logger.debug("cached GOROOT update signal caught")

        val project = module.project
        val settings = project.tinyGoConfiguration()
        if (!settings.enabled) return

        val tinyGoSettings: TinyGoConfiguration = ConfigurationWithHistory(project)
        TinyGoServiceScope.getScope(project).launch {
            val output = project.service<TinyGoInfoExtractor>()
                .extractTinyGoInfo(tinyGoSettings, CachedGoRootInvalidator(project))
                ?: return@launch
            tinyGoSettings.extractTinyGoInfo(output)
            project.service<TinyGoLibraryLayoutService>().refresh(tinyGoSettings)
            edtWriteAction {
                tinyGoSettings.saveState(project)

                propagateGoFlags(project, tinyGoSettings)
                updateExtLibrariesAndCleanCache(project)
            }
        }

        logger.debug("cached GOROOT update signal processed")
    }
}

interface TinyGoExtractionFailureListener : EventListener {
    fun onExtractionFailure()
}

class CachedGoRootInvalidator(private val project: Project) : TinyGoExtractionFailureListener {
    override fun onExtractionFailure() {
        project.service<TinyGoLibraryLayoutService>().clear()
        val tinyGoSettings = project.tinyGoConfiguration()
        tinyGoSettings.cachedGoRoot = GoSdk.NULL
        tinyGoSettings.saveState(project)
        updateExtLibrariesAndCleanCache(project)
    }
}

@RequiresEdt
internal fun updateExtLibrariesAndCleanCache(project: Project) {
    if (!project.isDisposed) {
        application.assertIsDispatchThread()
        project.service<GoSdkService>().incModificationCount()
        GoUtil.cleanResolveCache(project)
        GoLibrariesUtil.updateLibraries(project, RootsChangeRescanningInfo.TOTAL_RESCAN, { }, null)
    }
}

fun sendReloadLibrariesSignal(project: Project) {
    if (!project.isDisposed) {
        val messageBus: MessageBus = project.messageBus
        val modules = ModuleManager.getInstance(project).modules
        modules.forEach { messageBus.syncPublisher(GoModuleSettings.BUILD_TARGET_TOPIC).changed(it, true) }
    }
}
