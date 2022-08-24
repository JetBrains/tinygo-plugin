package org.jetbrains.tinygoplugin.heapAllocations

import com.goide.execution.GoWslUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
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

fun supplyHeapAllocsFromOutput(module: Module, processOutput: String): Map<String, Set<TinyGoHeapAlloc>> {
    val result = mutableMapOf<String, MutableSet<TinyGoHeapAlloc>>()

    val heapAllocRegex = Regex("(/.+/+.+.go):([0-9]+):([0-9]+): (.+)")
    val matches = heapAllocRegex.findAll(processOutput)
    val tinyGoSettings = module.project.tinyGoConfiguration()
    val tinyGoSdkRoot = tinyGoSettings.sdk.sdkRoot!!
    for (match in matches) {
        var pathCandidate = match.groupValues[1]
        val wsl = GoWslUtil.getWsl(module)
        if (wsl != null) pathCandidate = wsl.getWindowsPath(pathCandidate)

        var file = VfsUtil.findFile(File(pathCandidate).toPath(), false)!!
        if (VfsUtil.isAncestor(tinyGoSdkRoot, file, false)) {
            val relativePath = VfsUtil.getRelativePath(file, tinyGoSdkRoot)
            if (relativePath != null) {
                file = tinyGoSettings.cachedGoRoot.sdkRoot?.findFileByRelativePath(relativePath)!!
            }
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
