package org.jetbrains.tinygoplugin.heapAllocations

import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import java.io.File

data class TinyGoHeapAlloc(
    val file: VirtualFile,
    val line: Int,
    val column: Int,
    val reason: String
) {
    override fun toString(): String = "${file.canonicalPath}:$line:$column"
}

@RequiresBackgroundThread
suspend fun supplyHeapAllocsFromOutput(module: Module, processOutput: String): Map<String, Set<TinyGoHeapAlloc>> {
    val result = mutableMapOf<String, MutableSet<TinyGoHeapAlloc>>()

    val heapAllocRegex = Regex("(/.+/+.+.go):([0-9]+):([0-9]+): (.+)")
    val matches = heapAllocRegex.findAll(processOutput)
    val tinyGoSettings = module.project.tinyGoConfiguration()
    val tinyGoSdkRoot = tinyGoSettings.sdk.sdkRoot!!
    for (match in matches) {
        val file = readAction {
            var f = VfsUtil.findFile(File(match.groupValues[1]).toPath(), false)!!
            if (VfsUtil.isAncestor(tinyGoSdkRoot, f, false)) {
                val relativePath = VfsUtil.getRelativePath(f, tinyGoSdkRoot)
                if (relativePath != null) {
                    f = tinyGoSettings.cachedGoRoot.sdkRoot?.findFileByRelativePath(relativePath)!!
                }
            }
            f
        }
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
