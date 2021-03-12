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

private fun filterCandidatesRecursive(searchDirs: Collection<String>): Set<String> {
    val tinyGoHomes = mutableListOf<String>()
    for (searchDir in searchDirs) {
        if (FileUtil.exists(searchDir)) {
            var candidates = arrayOf(File(searchDir))
            if (!checkDirectoryForTinyGo(candidates.first())) {
                candidates = File(searchDir).listFiles { child -> checkDirectoryForTinyGo(child) } ?: emptyArray()
            }
            tinyGoHomes.addAll(candidates.map { f -> f.path })
        }
    }
    return tinyGoHomes.toSet()
}

fun suggestSdkDirectories(): Set<File> {
    val searchDirs = mutableListOf<String>()
    when {
        GoOsManager.isLinux() -> {
            searchDirs.add("/usr/local/tinygo")
        }
        GoOsManager.isMac() -> {
            val macSearchDirs = arrayListOf(
                "/opt/local/lib/tinygo",
                "/usr/local/Cellar/tinygo"
            )
            searchDirs.addAll(macSearchDirs)
        }
        GoOsManager.isWindows() -> {
            val winSearchDirs = arrayListOf(
                "${System.getProperty("user.home")}\\scoop\\apps\\tinygo",
                "${System.getenv("SCOOP")}\\tinygo",
                "${System.getenv("SCOOP_GLOBAL")}\\tinygo",
                "C:\\tinygo",
                "C:\\Program Files\\tinygo",
                "C:\\Program Files (x86)\\tinygo"
            )
            searchDirs.addAll(winSearchDirs)
        }
    }
    val tinyGoSdkHomes = filterCandidatesRecursive(searchDirs) + searchInSystemPath()
    return tinyGoSdkHomes.map { tinyGoHome -> File(tinyGoHome) }.toSortedSet()
}

fun suggestSdkDirectory(): File? = suggestSdkDirectories().firstOrNull()
