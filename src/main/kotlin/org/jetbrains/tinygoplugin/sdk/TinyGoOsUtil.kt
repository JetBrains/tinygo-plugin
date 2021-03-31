package org.jetbrains.tinygoplugin.sdk

import com.goide.GoOsManager
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Paths

interface OSUtils {
    fun suggestedDirectories(): Collection<String>
    fun executablePath(tinyGoSDKPath: String): String
    fun executableName(): String
    fun executableBaseName(): String = "tinygo"
}

internal abstract class OSUtilsImpl : OSUtils {
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
