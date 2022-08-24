package org.jetbrains.tinygoplugin.sdk

import com.goide.GoConstants
import com.goide.GoOsManager
import com.goide.execution.GoWslUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

interface OSUtils {
    fun osName(): String
    fun suggestedDirectories(): Collection<String>
    fun executableName(): String
    fun executableBaseName(): String = "tinygo"
    fun executableVFile(sdkRoot: VirtualFile?): VirtualFile?
    fun emulatedArch(arch: String): String = arch
    fun execute(command: String, vararg args: String): ProcessOutput
}

internal abstract class OSUtilsImpl : OSUtils {
    override fun execute(command: String, vararg args: String): ProcessOutput {
        val commandLine = GeneralCommandLine(command, *args)
        return ExecUtil.execAndGetOutput(commandLine)
    }

    override fun executableVFile(sdkRoot: VirtualFile?): VirtualFile? =
        sdkRoot?.findChild("bin")?.findChild(executableName())
}

internal class WindowsUtils : OSUtilsImpl() {
    override fun osName(): String = GoConstants.WINDOWS_OS

    override fun suggestedDirectories(): Collection<String> =
        listOf(
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

const val UNKNOWN_OS = "unknown"
internal class UnknownOSUtils : UnixUtils() {
    override fun osName(): String = UNKNOWN_OS

    override fun suggestedDirectories(): Collection<String> =
        emptyList()
}

internal class MacOSUtils : UnixUtils() {
    override fun osName(): String = GoConstants.DARWIN_OS

    override fun suggestedDirectories(): Collection<String> {
        val macPorts = "/opt/local/lib/tinygo"
        val homeBrew = "/usr/local/Cellar/tinygo"
        val file = FileUtil.findFirstThatExist(macPorts, homeBrew)
        val tinyGoSdkDirectories = file?.canonicalFile?.listFiles { child ->
            checkDirectoryForTinyGo(child)
        }
        return if (tinyGoSdkDirectories.isNullOrEmpty()) emptyList() else tinyGoSdkDirectories.map { f -> f.path }
    }

    override fun emulatedArch(arch: String): String =
        if (arch == "arm64") "amd64" else arch
}

internal open class LinuxUtils : UnixUtils() {
    override fun osName(): String = GoConstants.LINUX_OS

    override fun suggestedDirectories(): Collection<String> = listOf("/usr/local/tinygo")
}

private const val WSL_EXEC_TIMEOUT = 1_000
internal class WSLUtils(private val wsl: WSLDistribution) : LinuxUtils() {
    override fun suggestedDirectories(): Collection<String> {
        val linuxPaths = super.suggestedDirectories()
        return linuxPaths.map { wsl.getWindowsPath(it) }
    }

    override fun execute(command: String, vararg args: String): ProcessOutput =
        wsl.executeOnWsl(WSL_EXEC_TIMEOUT, command, *args)
}

fun parseOsManager(osName: String): OSUtils {
    return when (osName) {
        GoConstants.LINUX_OS -> {
            LinuxUtils()
        }

        GoConstants.DARWIN_OS -> {
            MacOSUtils()
        }

        GoConstants.WINDOWS_OS -> {
            WindowsUtils()
        }

        else -> {
            UnknownOSUtils()
        }
    }
}

fun osManagerIn(pathContext: String?): OSUtils =
    when {
        GoOsManager.isLinux() -> {
            LinuxUtils()
        }

        GoOsManager.isMac() -> {
            MacOSUtils()
        }

        GoOsManager.isWindows() -> {
            val wsl = GoWslUtil.getWsl(VfsUtilCore.urlToPath(pathContext))
            if (wsl != null) WSLUtils(wsl) else WindowsUtils()
        }

        else -> {
            UnknownOSUtils()
        }
    }

fun patchForWSLIfNeeded(filePath: String?): String {
    if (filePath == null) return ""
    return GoWslUtil
        .getWsl(filePath)
        ?.getWslPath(filePath)
        ?: filePath
}
