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
import java.io.IOException
import java.util.stream.Collectors

fun notifyTinyGoNotConfigured(
    project: Project?,
    content: String,
    notificationType: NotificationType = NotificationType.INFORMATION,
    goSdkConfigurationNeeded: Boolean = false
) {
    val notification = GoNotifications.getGeneralGroup()
        .createNotification("TinyGo SDK configuration incomplete", content, notificationType)
    if (project != null) {
        if (goSdkConfigurationNeeded) {
            notification.addAction(object : NotificationAction("Go SDK settings") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    ShowSettingsUtil.getInstance().editConfigurable(project, GoSdkConfigurable(project, true))
                }
            })
        }
        notification.addAction(object : NotificationAction("TinyGo settings") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                ShowSettingsUtil.getInstance().editConfigurable(project, TinyGoSettingsService(project))
            }
        })
    }
    notification.notify(project)
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
            val realTinyGoExec = File(candidate).toPath().toRealPath()
            if (!realTinyGoExec.exists()) {
                continue
            }
            val realTinyGoBin = realTinyGoExec.parent
            val realTinyGoHome = realTinyGoBin.parent.toFile()
            val directoryContainsTinyGo = checkDirectoryForTinyGo(realTinyGoHome)
            if (directoryContainsTinyGo && tinyGoHomesInSystemPath.add(realTinyGoHome.path)) {
                Logger.getInstance("Detecting TinyGo").info(
                    "TinyGo home found: candidate from PATH $candidate located in ${realTinyGoHome.path}"
                )
            }
        } catch (ex: IOException) {
            Logger.getInstance("Detecting TinyGo").info("$candidate does not exist")
        }
    }
    return tinyGoHomesInSystemPath
}

@Suppress("SpreadOperator")
fun suggestSdkDirectories(): Set<File> {
    val tinyGoSdkHomeCandidates: MutableSet<String> = mutableSetOf()
    if (GoOsManager.isLinux()) {
        tinyGoSdkHomeCandidates.add("/usr/local/tinygo")
    } else if (GoOsManager.isMac()) {
        val macPorts = "/opt/local/lib/tinygo"
        val homeBrew = "/usr/local/Cellar/tinygo"
        val file = FileUtil.findFirstThatExist(macPorts, homeBrew)
        if (file != null) {
            val tinyGoSdkDirectories = file.canonicalFile.listFiles { child -> checkDirectoryForTinyGo(child) }
            if (!tinyGoSdkDirectories.isNullOrEmpty()) {
                tinyGoSdkHomeCandidates.addAll(tinyGoSdkDirectories.map { f -> f.path })
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
        for (tinyGoSdkHomeCandidate in tinyGoSdkHomeCandidates) {
            if (!FileUtil.exists(tinyGoSdkHomeCandidate)) {
                tinyGoSdkHomeCandidates.remove(tinyGoSdkHomeCandidate)
            }
        }
        return tinyGoSdkHomeCandidates.stream().map { path -> File(path) }.collect(Collectors.toSet())
    } else emptySet()
}

fun suggestSdkDirectory(): File? = suggestSdkDirectories().firstOrNull()
