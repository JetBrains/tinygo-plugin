package org.jetbrains.tinygoplugin.heapAllocations

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

data class TinyGoHeapAlloc(
    val file: VirtualFile,
    val line: Int,
    val column: Int,
    val reason: String
) {
    override fun toString(): String = "${file.canonicalPath}:$line:$column"
}

fun supplyHeapAllocsFromOutput(processOutput: String): Map<String, Set<TinyGoHeapAlloc>> {
    val result = mutableMapOf<String, MutableSet<TinyGoHeapAlloc>>()

    val heapAllocRegex = Regex("(/.+/+.+.go):([0-9]+):([0-9]+): (.+)")
    val matches = heapAllocRegex.findAll(processOutput)
    for (match in matches) {
        val file = VfsUtil.findFile(File(match.groupValues[1]).toPath(), false)!!
        val parentDir = file.parent.canonicalPath!!
        result.putIfAbsent(parentDir, mutableSetOf())
        result[parentDir]?.add(
            @Suppress("MagicNumber")
            TinyGoHeapAlloc(
                file,
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
                match.groupValues[4]
            )
        )
    }

    return result
}
