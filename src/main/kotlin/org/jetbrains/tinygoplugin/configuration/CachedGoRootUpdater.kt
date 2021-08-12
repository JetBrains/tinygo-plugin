package org.jetbrains.tinygoplugin.configuration

import com.goide.GoLibrariesUtil
import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdkService
import com.goide.util.GoUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.EmptyRunnable
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.services.propagateGoFlags

internal class CachedGoRootUpdater : GoModuleSettings.BuildTargetListener {
    override fun changed(module: Module, batchUpdate: Boolean) {
        val project = module.project
        val settings = TinyGoConfiguration.getInstance(project)
        if (!settings.enabled) return

        val infoExtractor = TinyGoInfoExtractor(project)
        val tinyGoSettings: TinyGoConfiguration = ConfigurationWithHistory(project)
        infoExtractor.extractTinyGoInfo(tinyGoSettings) { _, output ->
            tinyGoSettings.extractTinyGoInfo(output)
            tinyGoSettings.saveState(project)

            propagateGoFlags(project, tinyGoSettings)
            if (!project.isDisposed) {
                ApplicationManager.getApplication().assertIsDispatchThread()
                GoSdkService.getInstance(project).incModificationCount()
                GoUtil.cleanResolveCache(project)
                GoLibrariesUtil.updateLibraries(project, EmptyRunnable.getInstance(), null)
            }
        }
    }
}
