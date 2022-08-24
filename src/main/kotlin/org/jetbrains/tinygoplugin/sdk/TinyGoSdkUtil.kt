package org.jetbrains.tinygoplugin.sdk

import com.goide.GoNotifications
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.services.editTinyGoSettingsLater
import java.io.File

const val CONFIGURATION_INCOMPLETE_NOTIFICATION = "notifications.tinygoSDK.configuration.incomplete"

fun notifyTinyGoNotConfigured(
    project: Project?,
    content: String,
) {
    val notification = GoNotifications.getGeneralGroup()
        .createNotification(
            TinyGoBundle.message(CONFIGURATION_INCOMPLETE_NOTIFICATION),
            content,
            NotificationType.WARNING
        )
    notification.addAction(object : NotificationAction("TinyGo settings") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            editTinyGoSettingsLater(project!!)
        }
    })
    notification.notify(project)
}

fun suggestSdkDirectories(pathContext: String?): Collection<File> {
    return osManagerIn(pathContext).suggestedDirectories().asSequence().map { File(it) }.filter(File::exists)
        .filter(::checkBin).filter(::checkTargets).filter(::checkMachinesSources).toList()
}

fun findTinyGoInPath(pathContext: String?): File? {
    val tinyGoExec =
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(
            osManagerIn(pathContext).executableBaseName()
        ) ?: return null
    // resolve links and go 2 directories up: -> bin -> tinygo
    return tinyGoExec.canonicalFile.parentFile?.parentFile
}

fun suggestSdkDirectory(pathContext: String?): File? {
    val tinyGoPath = findTinyGoInPath(pathContext)
    if (tinyGoPath != null) {
        return tinyGoPath
    }
    return suggestSdkDirectories(pathContext).firstOrNull()
}
