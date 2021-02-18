package org.jetbrains.tinygoplugin.sdk

import com.goide.GoNotifications
import com.goide.GoOsManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService
import java.io.File

class TinyGoSdkUtil private constructor() {
    companion object {
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
                if (binDirCandidates != null) {
                    if (binDirCandidates.isNotEmpty()) {
                        val binDir = binDirCandidates.first()
                        // research if other criteria possible
                        return binDir?.listFiles { child ->
                            child.isFile && child.canExecute() && child.name.endsWith("tinygo")
                        }?.isNotEmpty() ?: false
                    }
                }
            }
            return false
        }

        fun suggestSdkDirectoryStr(): String = suggestSdkDirectory()?.canonicalPath ?: ""

        private fun suggestSdkDirectory(): VirtualFile? {
            if (GoOsManager.isLinux()) {
                // must be extended and tested on Linux
                val usrLibs = "/usr/local/tinygo"
                if (FileUtil.exists(usrLibs)) {
                    if (checkDirectoryForTinyGo(File(usrLibs))) {
                        return LocalFileSystem.getInstance().findFileByPath(usrLibs)
                    }
                }
            } else if (GoOsManager.isMac()) {
                val macPorts = "/opt/local/lib/tinygo"
                val homeBrew = "/usr/local/Cellar/tinygo"
                val file = FileUtil.findFirstThatExist(macPorts, homeBrew)
                if (file != null) {
                    val tinyGoSdkDirectories = file.canonicalFile.listFiles {
                            child -> checkDirectoryForTinyGo(child)
                    }?.first()
                    if (tinyGoSdkDirectories != null) {
                        return LocalFileSystem.getInstance().findFileByIoFile(tinyGoSdkDirectories)
                    }
                }
            } else if (GoOsManager.isWindows()) {
                // must be tested under Windows
                val winScoopInstallDir = "${System.getenv("SCOOP")}\\tinygo"
                val winScoopGlobalInstallDir = "${System.getenv("SCOOP_GLOBAL")}\\tinygo"
                val winC = "C:\\tinygo"
                val winProgramFiles = "C:\\Program Files\\tinygo"
                val winProgramFilesX86 = "C:\\Program Files (x86)\\tinygo"
                val file = FileUtil.findFirstThatExist(
                    winScoopInstallDir, winScoopGlobalInstallDir, winC, winProgramFiles, winProgramFilesX86
                )
                if (file != null) {
                    if (checkDirectoryForTinyGo(file)) {
                        return LocalFileSystem.getInstance().findFileByIoFile(file)
                    }
                }
            }
            return null
        }
    }
}
