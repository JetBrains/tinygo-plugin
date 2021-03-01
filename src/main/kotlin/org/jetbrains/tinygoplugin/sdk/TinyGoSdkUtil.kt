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
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService
import java.io.File
import java.nio.file.Paths

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

@Suppress("SpreadOperator")
fun suggestSdkDirectory(): File? {
    val tinyGoSdkHomeCandidates: Collection<String> = osManager.suggestedDirectories()

    return if (GoOsManager.isLinux() || GoOsManager.isMac() || GoOsManager.isWindows()) {
        if (tinyGoSdkHomeCandidates.isNotEmpty()) {
            FileUtil.findFirstThatExist(*tinyGoSdkHomeCandidates.toTypedArray())
        } else null
    } else null
}

interface OSUtils {
    fun suggestedDirectories(): Collection<String>
    fun executablePath(tinyGoSDKPath: String): String
}

internal abstract class OSUtilsImpl : OSUtils {
    protected abstract fun executableName(): String
    override fun executablePath(tinyGoSDKPath: String): String {
        val tinyGoRoot =
            Paths.get(tinyGoSDKPath).toAbsolutePath().toString()
        return Paths.get(
            tinyGoRoot,
            "bin",
            executableName()
        ).toString()
    }
}

internal class WindowsUtils : OSUtilsImpl() {
    override fun suggestedDirectories(): Collection<String> =
        arrayListOf(
            "${System.getenv("SCOOP")}\\tinygo",
            "${System.getenv("SCOOP_GLOBAL")}\\tinygo",
            "C:\\tinygo",
            "C:\\Program Files\\tinygo",
            "C:\\Program Files (x86)\\tinygo"
        )

    override fun executableName(): String {
        return "tinygo.exe"
    }
}

internal abstract class UnixUtils : OSUtilsImpl() {
    override fun executableName(): String =
        "tinygo"
}

internal class UnknownOSUtils : UnixUtils() {
    override fun suggestedDirectories(): Collection<String> =
        emptyList()
}

internal class MacOSUtils : UnixUtils() {
    override fun suggestedDirectories(): Collection<String> {
        val macPorts = "/opt/local/lib/tinygo"
        val homeBrew = "/usr/local/Cellar/tinygo"
        val file = FileUtil.findFirstThatExist(macPorts, homeBrew)
        val tinyGoSdkDirectories = file?.canonicalFile?.listFiles { child ->
            checkDirectoryForTinyGo(child)
        }
        return if (tinyGoSdkDirectories.isNullOrEmpty()) emptyList() else tinyGoSdkDirectories.map { f -> f.path }
    }
}

internal class LinuxUtils : UnixUtils() {
    override fun suggestedDirectories(): Collection<String> = listOf("/usr/local/tinygo")
}

val osManager: OSUtils
    get() {
        return when {
            GoOsManager.isLinux() -> {
                LinuxUtils()
            }
            GoOsManager.isMac() -> {
                MacOSUtils()
            }
            GoOsManager.isWindows() -> {
                WindowsUtils()
            }
            else -> {
                UnknownOSUtils()
            }
        }
    }
