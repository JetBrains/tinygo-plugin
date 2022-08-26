package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.intellij.execution.RunManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.runconfig.TinyGoRunConfiguration

fun editTinyGoSettingsLater(project: Project, afterSettingsApply: () -> Unit = {}) {
    ApplicationManager.getApplication().invokeLater {
        if (service<ShowSettingsUtil>().editConfigurable(project, TinyGoSettingsService(project))) {
            afterSettingsApply.invoke()
        }
    }
}
fun propagateGoFlags(project: Project, settings: TinyGoConfiguration) {
    val goSettings = ModuleManager.getInstance(project).modules.mapNotNull {
        it?.getService(GoModuleSettings::class.java)
    }.firstOrNull()
    if (goSettings == null) {
        TinyGoSettingsService.logger.warn("Could not find go module settings")
        return
    }
    val buildSettings = goSettings.buildTargetSettings
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
