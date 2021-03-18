package org.jetbrains.tinygoplugin.sdk

import com.goide.GoNotifications
import com.goide.GoOsManager
import com.goide.configuration.GoSdkConfigurable
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService
import java.io.File

fun notifyTinyGoNotConfigured(
    project: Project?,
    content: String,
    notificationType: NotificationType = NotificationType.INFORMATION,
    goSdkConfigurationNeeded: Boolean = false,
) {
    val notification = GoNotifications.getGeneralGroup()
        .createNotification("TinyGo SDK configuration incomplete", content, NotificationType.INFORMATION)
    notification.addAction(object : NotificationAction("TinyGo settings") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            ShowSettingsUtil.getInstance().editConfigurable(project, TinyGoSettingsService(project!!))
        }
    })
    notification.notify(project)
}

fun getTinyGoExecutable(sdkRoot: VirtualFile?): VirtualFile? {
    if (sdkRoot != null && sdkRoot.isValid && sdkRoot.isDirectory) {
        val sdkBinDir = sdkRoot.findChild("bin")
        if (sdkBinDir != null && sdkBinDir.isValid && sdkBinDir.isDirectory) {
            val sdkTinyGoExecutable = sdkBinDir.findChild("tinygo")
            if (sdkTinyGoExecutable != null && sdkTinyGoExecutable.isValid) {
                return sdkTinyGoExecutable
            }
        }
    }
    return null
}

fun checkDirectoryForTinyGo(dir: VirtualFile): Boolean {
    val path = dir.canonicalPath
    if (path != null) {
        val file = File(path)
        return checkDirectoryForTinyGo(file)
    }
    return false
}

private fun checkDirectoryForTinyGo(dir: File): Boolean {
    if (dir.isDirectory) {
        val binDirCandidates = dir.listFiles { child -> child.isDirectory && child.name.endsWith("bin") }
        if (binDirCandidates != null && binDirCandidates.isNotEmpty()) {
            val binDir = binDirCandidates.first()
            // research if other criteria possible
            return binDir?.listFiles { child ->
                child.isFile && child.canExecute() && child.name.endsWith("tinygo")
            }?.isNotEmpty() ?: false
        }
    }
    return false
}

fun suggestSdkDirectoryStr(): String = suggestSdkDirectory()?.canonicalPath ?: ""

fun suggestSdkDirectories(): Collection<File> {
    return osManager.suggestedDirectories().asSequence().map { File(it) }.filter(File::exists)
        .filter(::checkBin).filter(::checkTargets).filter(::checkMachinesSources).toList()
}

fun findTinyGoInPath(): File? {
    val tinygoExec =
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(osManager.executableBaseName()) ?: return null
    // resolve links and go 2 directories up: -> bin -> tinygo
    return tinygoExec.canonicalFile.parentFile?.parentFile
}

fun suggestSdkDirectory(): File? {
    val tinygoPath = findTinyGoInPath()
    if (tinygoPath != null) {
        return tinygoPath
    }
    return suggestSdkDirectories().firstOrNull()
}
