package org.jetbrains.tinygoplugin.sdk

import com.goide.sdk.GoBasedSdk
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import org.jetbrains.tinygoplugin.services.TinyGoExecutable
import java.net.URL
import java.util.Objects
import javax.swing.Icon

fun tinyGoSdkVersion(versionString: String?): TinyGoSdkVersion {
    val logger = getLogger<TinyGoSdkVersion>()
    val numbers = versionString?.split('.')
    if (numbers == null) {
        logger.warn { "Null version provided" }
        return unknownVersion
    } else if (numbers.size != 3) {
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
        private const val maxVersion = 100
    }

    fun isAtLeast(version: TinyGoSdkVersion): Boolean {
        return version != unknownVersion && version.toInt() <= toInt()
    }

    fun isLessThan(version: TinyGoSdkVersion): Boolean {
        return version != unknownVersion && version.toInt() > toInt()
    }

    private fun toInt(): Int {
        return patch + minor * maxVersion + major * maxVersion * maxVersion
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }
}

@Suppress("TooManyFunctions")
open class TinyGoSdk(
    protected val tinyGoHomeUrl: String?,
    protected val tinyGoVersion: String?,
) : GoBasedSdk {

    val sdkRoot: VirtualFile? = if (tinyGoHomeUrl != null) {
        VfsUtil.findFileByURL(URL(tinyGoHomeUrl))
    } else {
        null
    }

    internal var sdkVersion: TinyGoSdkVersion = tinyGoSdkVersion(tinyGoVersion)

    override fun getIcon(): Icon = TinyGoPluginIcons.TinyGoIcon

    override fun getVersion(): String = sdkVersion.toString()

    override fun getHomeUrl(): String = tinyGoHomeUrl ?: ""

    override fun getSrcDir(): VirtualFile? = sdkRoot?.findChild("src")

    fun root(): VirtualFile? = sdkRoot

    // TODO: move function here
    override fun getExecutable(): VirtualFile? = getTinyGoExecutable(sdkRoot)

    override fun isValid(): Boolean {
        val sources = srcDir
        return sources != null && sources.isValid && sources.isInLocalFileSystem && sources.isDirectory
    }

    override fun getName(): String = "TinyGo $version"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val sdk = other as TinyGoSdk
        return FileUtil.comparePaths(sdk.homeUrl, homeUrl) == 0
    }

    override fun hashCode(): Int = Objects.hash(tinyGoHomeUrl)
}

fun TinyGoSdk.computeVersion(project: Project, onFinish: () -> Unit) {
    val sdkRoot = this.sdkRoot?.canonicalPath ?: return
    TinyGoExecutable(project).execute(sdkRoot, listOf("version")) { _, output ->
        val re = """tinygo version (\d+.\d+.\d+)"""
        val match = re.toRegex().find(output)
        if (match != null) {
            sdkVersion = tinyGoSdkVersion(match.groupValues[1])
        }
        onFinish()
    }
}

val nullSdk = TinyGoSdk(null, unknownVersion.toString())
