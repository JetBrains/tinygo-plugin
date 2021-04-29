package org.jetbrains.tinygoplugin.services

import com.intellij.util.io.exists
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.nullSdk
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

fun tinyGoTargets(sdk: TinyGoSdk): Set<String> {
    if (sdk == nullSdk) {
        return emptySet()
    }
    val sdkPath = sdk.sdkRoot?.toNioPath() ?: return emptySet()
    val targetsFolder = Paths.get(sdkPath.toString(), "targets")
    if (!targetsFolder.exists()) {
        return emptySet()
    }
    return Files.list(targetsFolder).map {
        it.fileName.toString()
    }.filter {
        it.endsWith(".json")
    }.map {
        it.substringBeforeLast(".json")
    }.collect(Collectors.toCollection {
        TreeSet()
    }) as TreeSet<String>
}
