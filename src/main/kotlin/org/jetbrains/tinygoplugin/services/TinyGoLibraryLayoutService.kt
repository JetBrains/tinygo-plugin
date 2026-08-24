package org.jetbrains.tinygoplugin.services

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.Strictness
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.updateExtLibrariesAndCleanCache
import java.io.StringReader
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong

internal data class TinyGoListPackage(
    @SerializedName("Dir") val directory: String,
    @SerializedName("ImportPath") val importPath: String,
    @SerializedName("GoFiles") val goFiles: List<String>?,
    @SerializedName("IgnoredGoFiles") val ignoredGoFiles: List<String>?,
)

internal fun parseTinyGoList(output: String): List<TinyGoListPackage> {
    val gson = Gson()
    val reader = JsonReader(StringReader(output)).apply { strictness = Strictness.LENIENT }
    return buildList {
        while (reader.peek() != JsonToken.END_DOCUMENT) {
            add(gson.fromJson(reader, TinyGoListPackage::class.java))
        }
    }
}

internal data class TinyGoDeviceLayout(
    val selectedDirectories: Set<Path>,
    val ignoredFiles: Set<Path>,
)

internal fun calculateDeviceLayout(
    deviceRoot: Path,
    packages: List<TinyGoListPackage>,
): TinyGoDeviceLayout {
    val normalizedRoot = deviceRoot.normalize()
    val devicePackages = packages.mapNotNull { packageInfo ->
        val importPath = packageInfo.importPath
        if (importPath != "device" && !importPath.startsWith("device/")) return@mapNotNull null
        val relativePath = importPath.removePrefix("device").trimStart('/')
        val packageDirectory = if (relativePath.isEmpty()) normalizedRoot else normalizedRoot.resolve(relativePath)
        packageInfo to packageDirectory
    }
    return TinyGoDeviceLayout(
        selectedDirectories = devicePackages.mapTo(mutableSetOf()) { it.second },
        ignoredFiles = devicePackages.flatMapTo(mutableSetOf()) { (packageInfo, packageDirectory) ->
            packageInfo.ignoredGoFiles.orEmpty().map(packageDirectory::resolve)
        },
    )
}

@Service(Service.Level.PROJECT)
internal class TinyGoLibraryLayoutService(private val project: Project) {
    private data class LayoutKey(
        val cachedGoRootUrl: String,
        val target: String,
        val tinyGoSdkUrl: String,
        val gc: String,
        val scheduler: String,
    )

    private data class Snapshot(
        val key: LayoutKey?,
        val excludedRoots: Set<VirtualFile>,
    ) {
        companion object {
            val EMPTY = Snapshot(null, emptySet())
        }
    }

    private val executor = TinyGoExecutable(project)
    private val generation = AtomicLong()
    private val refreshMutex = Mutex()

    @Volatile
    private var snapshot = Snapshot.EMPTY

    fun getExcludedRoots(cachedGoRoot: VirtualFile, settings: TinyGoConfiguration): Set<VirtualFile> {
        val current = snapshot
        return if (current.key == layoutKey(settings, cachedGoRoot)) {
            current.excludedRoots
        } else {
            conservativeExclusions(cachedGoRoot)
        }
    }

    fun requestRefresh(
        settings: TinyGoConfiguration,
        cachedGoRoot: VirtualFile,
    ) {
        if (snapshot.key == layoutKey(settings, cachedGoRoot)) return
        TinyGoServiceScope.getScope(project).launch {
            if (refresh(settings, cachedGoRoot)) {
                edtWriteAction {
                    updateExtLibrariesAndCleanCache(project)
                }
            }
        }
    }

    suspend fun refresh(
        settings: TinyGoConfiguration,
        cachedGoRootHint: VirtualFile? = null,
    ): Boolean = refreshMutex.withLock {
        refreshLocked(settings, cachedGoRootHint)
    }

    private suspend fun refreshLocked(
        settings: TinyGoConfiguration,
        cachedGoRootHint: VirtualFile?,
    ): Boolean {
        val currentGeneration = generation.incrementAndGet()
        val target = settings.targetPlatform
        val (sdkRoot, configuredCachedGoRoot) = readAction {
            settings.sdk.sdkRoot to settings.cachedGoRoot.sdkRoot
        }
        val cachedGoRoot = configuredCachedGoRoot ?: cachedGoRootHint
        if (cachedGoRoot == null) {
            publishIfCurrent(currentGeneration, Snapshot.EMPTY)
            return false
        }
        val key = layoutKey(settings, cachedGoRoot)
        if (snapshot.key == key) return false
        val previousExclusions = getExcludedRoots(cachedGoRoot, settings)
        val (excludedRoots, cacheable) = if (sdkRoot == null || target.isEmpty()) {
            readAction { conservativeExclusions(cachedGoRoot) } to true
        } else {
            val packages = listPackages(settings, sdkRoot)
            if (packages == null) {
                logger<TinyGoLibraryLayoutService>().warn(
                    "Cannot calculate library exclusions for TinyGo target '$target'"
                )
                readAction { conservativeExclusions(cachedGoRoot) } to false
            } else {
                readAction { calculateExcludedRoots(cachedGoRoot, packages) } to true
            }
        }
        val publishedKey = key.takeIf { cacheable }
        val published = publishIfCurrent(currentGeneration, Snapshot(publishedKey, excludedRoots))
        return published && previousExclusions != excludedRoots
    }

    private suspend fun listPackages(
        settings: TinyGoConfiguration,
        sdkRoot: VirtualFile,
    ): List<TinyGoListPackage>? {
        val arguments = buildList {
            add("list")
            addAll(tinyGoArguments(settings))
            add("-deps")
            add("-json")
            add("machine")
        }
        val execution = executor.execute(sdkRoot, arguments)
        return try {
            execution?.takeIf(TinyGoCommandResult::isSuccessful)?.stdout?.let(::parseTinyGoList)
        } catch (e: JsonParseException) {
            logger<TinyGoLibraryLayoutService>().warn("Cannot parse TinyGo package list", e)
            null
        }
    }

    fun clear() {
        generation.incrementAndGet()
        snapshot = Snapshot.EMPTY
    }

    private fun publishIfCurrent(currentGeneration: Long, newSnapshot: Snapshot): Boolean {
        if (generation.get() != currentGeneration || project.isDisposed) return false
        snapshot = newSnapshot
        return true
    }

    private fun layoutKey(settings: TinyGoConfiguration, cachedGoRoot: VirtualFile) = LayoutKey(
        cachedGoRootUrl = cachedGoRoot.url,
        target = settings.targetPlatform,
        tinyGoSdkUrl = settings.sdk.homeUrl,
        gc = settings.gc.cmd,
        scheduler = settings.scheduler.cmd,
    )

    private fun conservativeExclusions(cachedGoRoot: VirtualFile): Set<VirtualFile> =
        setOfNotNull(cachedGoRoot.findFileByRelativePath("src/device"))

    private fun calculateExcludedRoots(
        cachedGoRoot: VirtualFile,
        packages: List<TinyGoListPackage>,
    ): Set<VirtualFile> {
        val deviceRoot = cachedGoRoot.findFileByRelativePath("src/device") ?: return emptySet()
        val deviceRootPath = Path.of(deviceRoot.path).normalize()
        val layout = calculateDeviceLayout(deviceRootPath, packages)

        return buildSet {
            deviceRoot.children
                .filter(VirtualFile::isDirectory)
                .filter { child ->
                    val childPath = Path.of(child.path).normalize()
                    layout.selectedDirectories.none { it.startsWith(childPath) }
                }
                .forEach(::add)

            layout.ignoredFiles.forEach { ignoredFile ->
                val relativePath = deviceRootPath.relativize(ignoredFile)
                    .joinToString("/") { it.toString() }
                deviceRoot.findFileByRelativePath(relativePath)?.let(::add)
            }
        }
    }
}
