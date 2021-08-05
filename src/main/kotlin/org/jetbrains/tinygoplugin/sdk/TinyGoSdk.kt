package org.jetbrains.tinygoplugin.sdk

import com.goide.sdk.GoBasedSdk
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import org.jetbrains.tinygoplugin.services.TinyGoExecutable
import java.net.URL
import java.util.Objects
import javax.swing.Icon

@Suppress("ReturnCount", "MagicNumber")
fun tinyGoSdkVersion(versionString: String?): TinyGoSdkVersion {
    val logger = getLogger<TinyGoSdkVersion>()
    val numbers = versionString?.split('.')
    if (numbers == null) {
        logger.warn { "Null version provided" }
        return unknownVersion
    }
    if (numbers.size != 3) {
        logger.warn { "Could not parse version: $versionString" }
        return unknownVersion
    }
    val numbersParsed = numbers.mapNotNull { it.toIntOrNull() }
    if (numbersParsed.size != 3) {
        logger.warn { "Invalid version format: $versionString" }
        return unknownVersion
    }
    return TinyGoSdkVersion(numbersParsed[0], numbersParsed[1], numbersParsed[2])
}

val unknownVersion = TinyGoSdkVersion()

data class TinyGoSdkVersion(
    var major: Int = 0,
    var minor: Int = 0,
    var patch: Int = 0,
) {
    companion object {
        private const val MAX_VERSION = 1024
    }

    fun isAtLeast(version: TinyGoSdkVersion): Boolean {
        return version != unknownVersion && version.toInt() <= toInt()
    }

    fun isLessThan(version: TinyGoSdkVersion): Boolean {
        return version != unknownVersion && version.toInt() > toInt()
    }

    private fun toInt(): Int {
        return patch + minor * MAX_VERSION + major * MAX_VERSION * MAX_VERSION
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }
}

@Suppress("TooManyFunctions")
open class TinyGoSdk(
    protected val tinyGoHomeUrl: String?,
    internal var sdkVersion: TinyGoSdkVersion = unknownVersion,
) : GoBasedSdk {

    constructor(tinyGoHomeUrl: String?, tinyGoVersion: String?) : this(
        tinyGoHomeUrl,
        tinyGoSdkVersion(tinyGoVersion)
    )

    val sdkRoot: VirtualFile? by lazy {
        tinyGoHomeUrl?.let { VirtualFileManager.getInstance().findFileByUrl(it) }
    }

    override fun getIcon(): Icon = TinyGoPluginIcons.TinyGoIcon

    override fun getVersion(): String = sdkVersion.toString()

    override fun getHomeUrl(): String = tinyGoHomeUrl ?: ""

    override fun getSrcDir(): VirtualFile? = sdkRoot?.findChild("src")

    override fun getExecutable(): VirtualFile? = osManager.executableVFile(sdkRoot)

    override fun isValid(): Boolean {
        val sources = srcDir
        return sources != null && sources.isDirectory && sources.isValid && sources.isInLocalFileSystem
    }

    override fun getName(): String = "TinyGo $version"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val sdk = other as TinyGoSdk
        return FileUtil.comparePaths(urlToPath(sdk.tinyGoHomeUrl), urlToPath(tinyGoHomeUrl)) == 0
    }

    override fun hashCode(): Int = Objects.hash(tinyGoHomeUrl)
}

private fun urlToPath(url: String?): String? = url?.let { URL(it).path }

const val TINY_GO_VERSION_REGEX = """tinygo version (\d+.\d+.\d+)"""
fun TinyGoSdk.computeVersion(project: Project, onFinish: () -> Unit) {
    val sdkRoot = this.sdkRoot?.canonicalPath ?: return
    TinyGoExecutable(project).execute(sdkRoot, listOf("version")) { _, output ->
        val match = TINY_GO_VERSION_REGEX.toRegex().find(output)
        if (match != null) {
            sdkVersion = tinyGoSdkVersion(match.groupValues[1])
        } else {
            thisLogger().warn("Cannot determine TinyGoSdk version")
        }
        onFinish()
    }
}

val nullSdk = TinyGoSdk(null, unknownVersion)
