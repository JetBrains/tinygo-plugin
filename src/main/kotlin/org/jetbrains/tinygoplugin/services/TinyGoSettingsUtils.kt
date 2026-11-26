package org.jetbrains.tinygoplugin.services

import com.goide.project.GoBuildTargetSettings
import com.goide.project.GoModuleSettings
import com.intellij.execution.RunManager
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.coroutines.launch
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.updateExtLibrariesAndCleanCache
import org.jetbrains.tinygoplugin.runconfig.TinyGoRunConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoDownloadSdkService
import org.jetbrains.tinygoplugin.sdk.TinyGoDownloadingSdk

/**
 * Waits for the download of the TinyGo SDK of [tinyGoSettings], runs `tinygo info`,
 * and saves the result as the TinyGo configuration and the Go build target of the project.
 */
fun extractTinyGoSettings(
    project: Project,
    tinyGoSettings: TinyGoConfiguration,
) {
    TinyGoServiceScope.getScope(project).launch {
        val downloadingSdk = tinyGoSettings.sdk as? TinyGoDownloadingSdk
        if (downloadingSdk != null) {
            val downloadService = service<TinyGoDownloadSdkService>()
            downloadService.downloadTinyGoSdk(downloadingSdk, project) { }
            tinyGoSettings.sdk = downloadService.awaitDownloadedSdk(downloadingSdk)
        }
        val output =
            project.service<TinyGoInfoExtractor>().extractTinyGoInfo(tinyGoSettings)
                ?: return@launch
        tinyGoSettings.extractTinyGoInfo(output)
        edtWriteAction {
            tinyGoSettings.saveState(project)
            propagateGoFlags(project, tinyGoSettings)
            updateExtLibrariesAndCleanCache(project)
        }
    }
}

fun propagateGoFlags(project: Project, settings: TinyGoConfiguration) {
    val goSettings =
        ModuleManager.getInstance(project).modules.firstOrNull()?.let {
            GoModuleSettings.getInstance(it)
        }
    if (goSettings == null) {
        logger<TinyGoSettingsService>().warn("Could not find go module settings")
        return
    }
    // A copy: the getter returns the cached state, and the setter does not save a state that equals itself
    val buildSettings = GoBuildTargetSettings()
    XmlSerializerUtil.copyBean(goSettings.buildTargetSettings, buildSettings)
    buildSettings.arch = settings.goArch
    buildSettings.os = settings.goOS
    buildSettings.customFlags = settings.goTags.split(' ').toTypedArray()
    goSettings.buildTargetSettings = buildSettings
}

fun updateTinyGoRunConfigurations(project: Project, settings: TinyGoConfiguration) {
    val configurations = RunManager.getInstance(project)
        .allConfigurationsList
        .filterIsInstance<TinyGoRunConfiguration>()
    configurations.forEach {
        it.runConfig.targetPlatform = settings.targetPlatform
        it.runConfig.scheduler = settings.scheduler
        it.runConfig.gc = settings.gc
    }
}

fun tinyGoArguments(settings: TinyGoConfiguration): List<String> {
    val parameters = mutableListOf("-target", settings.targetPlatform)
    if (settings.scheduler != Scheduler.AUTO_DETECT) {
        parameters.addAll(listOf("-scheduler", settings.scheduler.cmd))
    }
    if (settings.gc != GarbageCollector.AUTO_DETECT) {
        parameters.addAll(listOf("-gc", settings.gc.cmd))
    }
    return parameters
}
