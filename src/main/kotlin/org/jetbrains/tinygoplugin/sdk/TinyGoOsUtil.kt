package org.jetbrains.tinygoplugin.sdk

import com.goide.GoOsManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.io.File

interface OSUtils {
    fun suggestedDirectories(): Collection<String>
    fun executableName(): String
    fun executableBaseName(): String = "tinygo"
    @RequiresBackgroundThread
    suspend fun executableVFile(sdkRoot: VirtualFile?): VirtualFile?
    @RequiresBackgroundThread
    suspend fun executablePath(tinyGoSDKPath: String): String {
        val file = readAction { VfsUtil.findFileByIoFile(File(tinyGoSDKPath), false) } ?: return ""
        return executableVFile(file)?.canonicalPath ?: ""
    }

    fun emulatedArch(arch: String): String = arch
}

internal abstract class OSUtilsImpl : OSUtils {

    override suspend fun executableVFile(sdkRoot: VirtualFile?): VirtualFile? = readAction {
        sdkRoot?.findChild("bin")?.findChild(executableName())
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

    override fun emulatedArch(arch: String): String =
        if (arch == "arm64") "amd64" else arch
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
