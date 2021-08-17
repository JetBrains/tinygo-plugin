package org.jetbrains.tinygoplugin.configuration

import com.goide.GoLibrariesUtil
import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.util.GoUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.EmptyRunnable
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.services.propagateGoFlags
import java.util.EventListener

internal class CachedGoRootUpdater : GoModuleSettings.BuildTargetListener {
    override fun changed(module: Module, batchUpdate: Boolean) {
        val project = module.project
        val settings = TinyGoConfiguration.getInstance(project)
        if (!settings.enabled) return

        val infoExtractor = TinyGoInfoExtractor(project)
        val tinyGoSettings: TinyGoConfiguration = ConfigurationWithHistory(project)
        infoExtractor.extractTinyGoInfo(tinyGoSettings, CachedGoRootInvalidator(project)) { _, output ->
            tinyGoSettings.extractTinyGoInfo(output)
            tinyGoSettings.saveState(project)

            propagateGoFlags(project, tinyGoSettings)
            updateExtLibrariesAndCleanCache(project)
        }
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
        ApplicationManager.getApplication().assertIsDispatchThread()
        GoSdkService.getInstance(project).incModificationCount()
        GoUtil.cleanResolveCache(project)
        GoLibrariesUtil.updateLibraries(project, EmptyRunnable.getInstance(), null)
    }
}
