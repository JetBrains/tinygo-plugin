package org.jetbrains.tinygoplugin.sdk

import com.goide.GoIcons
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkVersion
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.Objects
import javax.swing.Icon

@Suppress("TooManyFunctions")
open class TinyGoSdk(
    protected val tinyGoHomeUrl: String,
    protected val tinyGoVersion: String?,
    protected val tinyGoVersionFilePath: String?
) : GoSdk {
    private val tinyGoMajorVersion: GoSdkVersion = GoSdkVersion.fromText(version)

    override fun getIcon(): Icon = GoIcons.ICON

    override fun getVersion(): String? = tinyGoVersion

    override fun getMajorVersion(): GoSdkVersion = tinyGoMajorVersion

    override fun getVersionFilePath(): String? = tinyGoVersionFilePath

    override fun getHomeUrl(): String = tinyGoHomeUrl

    override fun getSdkRoot(): VirtualFile? = LocalFileSystem.getInstance().findFileByPath(tinyGoHomeUrl)

    override fun getSrcDir(): VirtualFile? = sdkRoot?.findChild("src")

    override fun getGoExecutable(): VirtualFile? = getTinyGoExecutable(sdkRoot)

    override fun getRootsToAttach(): MutableCollection<VirtualFile> {
        val srcDir = srcDir
        return if (srcDir != null && srcDir.isValid) mutableSetOf(srcDir) else mutableListOf()
    }

    override fun isValid(): Boolean {
        val srcDir = srcDir
        return srcDir != null && srcDir.isValid && srcDir.isInLocalFileSystem && srcDir.isDirectory
    }

    override fun isAppEngine(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val sdk = other as TinyGoSdk
        return FileUtil.comparePaths(sdk.homeUrl, homeUrl) == 0
    }

    override fun hashCode(): Int = Objects.hash(tinyGoHomeUrl)
}
