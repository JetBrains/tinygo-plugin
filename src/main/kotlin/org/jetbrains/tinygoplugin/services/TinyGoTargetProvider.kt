package org.jetbrains.tinygoplugin.services

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.TreeSet
import java.util.stream.Collectors
import kotlin.io.path.exists

@Suppress("ReturnCount")
@RequiresBackgroundThread
fun tinyGoTargets(sdkPath: Path): Set<String> {
    val targetsFolder = Paths.get(sdkPath.toString(), "targets")
    if (!targetsFolder.exists()) {
        return emptySet()
    }
    return Files.list(targetsFolder).use { targetStream ->
        targetStream.map {
            it.fileName.toString()
        }.filter {
            it.endsWith(".json")
        }.map {
            it.substringBeforeLast(".json")
        }.collect(
            Collectors.toCollection {
                TreeSet()
            }
        ) as TreeSet<String>
    }
}
