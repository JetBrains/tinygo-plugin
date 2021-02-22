package org.jetbrains.tinygoplugin.sdk

import com.goide.sdk.download.GoDownloadingSdk
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.util.Objects

class TinyGoDownloadingSdk(tinyGoVersion: String?, targetPath: String?) :
    TinyGoSdk(
        tinyGoHomeUrl = VfsUtilCore.pathToUrl(FileUtil.join(targetPath, tinyGoVersion)),
        tinyGoVersion = tinyGoVersion,
        tinyGoVersionFilePath = FileUtil.join(targetPath, tinyGoVersion)
    ) {

    var isDownloaded: Boolean = false

    override fun getSrcDir(): VirtualFile? = null

    override fun getGoExecutable(): VirtualFile? = null

    override fun getRootsToAttach(): MutableCollection<VirtualFile> = mutableSetOf()

    override fun isValid(): Boolean = true

    override fun getVersionFilePath(): String? = null

    fun toLocalTinyGoSdk(): TinyGoSdk = TinyGoSdk(tinyGoHomeUrl, tinyGoVersion, tinyGoVersionFilePath)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val sdk = other as GoDownloadingSdk
        return tinyGoVersion.equals(sdk.version) && tinyGoHomeUrl == sdk.homeUrl
    }

    override fun hashCode(): Int = Objects.hash(tinyGoVersion, tinyGoHomeUrl)
}
