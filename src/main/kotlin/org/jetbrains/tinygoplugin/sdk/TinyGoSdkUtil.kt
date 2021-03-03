package org.jetbrains.tinygoplugin.sdk

import com.goide.GoNotifications
import com.goide.GoOsManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService
import java.io.File
import java.io.IOException

fun notifyTinyGoNotConfigured(project: Project?, content: String) {
    val notification = GoNotifications.getGeneralGroup()
        .createNotification("TinyGo SDK configuration incomplete", content, NotificationType.INFORMATION)
    notification.addAction(object : NotificationAction("TinyGo settings") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            ShowSettingsUtil.getInstance().editConfigurable(project, TinyGoSettingsService(project!!))
        }
    })
    notification.notify(project)
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

private const val TINYGO_EXECUTABLE = "tinygo"

private fun searchInSystemPath(): Set<String> {
    Logger.getInstance("Detecting TinyGo").info("Started searching TinyGo home in PATH")

    val systemPathValues = System.getenv("PATH").split(":")
    val tinyGoHomesInSystemPath = mutableSetOf<String>()
    for (pathDir in systemPathValues) {
        val candidate = FileUtil.join(pathDir, TINYGO_EXECUTABLE)
        try {
            val realPath = File(candidate).toPath().toRealPath()
            if (realPath.exists() && checkDirectoryForTinyGo(realPath.parent.parent.toFile())) {
                if (tinyGoHomesInSystemPath.add(realPath.parent.parent.toFile().path)) {
                    Logger.getInstance("Detecting TinyGo")
                        .info(
                            "TinyGo home found: candidate from PATH $candidate " +
                                    "located in ${realPath.parent.parent.toFile().path}"
                        )
                }
            }
        } catch (ex: IOException) {
            Logger.getInstance("Detecting TinyGo").info("$candidate does not exist")
        }
    }
    return tinyGoHomesInSystemPath
}

@Suppress("SpreadOperator")
fun suggestSdkDirectory(): File? {
    val tinyGoSdkHomeCandidates: MutableSet<String> = mutableSetOf()
    if (GoOsManager.isLinux()) {
        tinyGoSdkHomeCandidates.add("/usr/local/tinygo")
    } else if (GoOsManager.isMac()) {
        val macPorts = "/opt/local/lib/tinygo"
        val homeBrew = "/usr/local/Cellar/tinygo"
        val file = FileUtil.findFirstThatExist(macPorts, homeBrew)
        if (file != null) {
            val tinyGoSdkDirectories = file.canonicalFile.listFiles { child ->
                checkDirectoryForTinyGo(child)
            }
            if (!tinyGoSdkDirectories.isNullOrEmpty()) {
                tinyGoSdkHomeCandidates.addAll(
                    tinyGoSdkDirectories.map { f -> f.path }
                )
            }
        }
    } else if (GoOsManager.isWindows()) {
        val winSearchDirs = arrayListOf(
            "${System.getenv("SCOOP")}\\tinygo",
            "${System.getenv("SCOOP_GLOBAL")}\\tinygo",
            "C:\\tinygo",
            "C:\\Program Files\\tinygo",
            "C:\\Program Files (x86)\\tinygo"
        )
        tinyGoSdkHomeCandidates.addAll(winSearchDirs)
    }

    return if (GoOsManager.isLinux() || GoOsManager.isMac() || GoOsManager.isWindows()) {
        tinyGoSdkHomeCandidates.addAll(searchInSystemPath())

        if (tinyGoSdkHomeCandidates.isNotEmpty()) {
            FileUtil.findFirstThatExist(*tinyGoSdkHomeCandidates.toTypedArray())
        } else null
    } else null
}
