package org.jetbrains.tinygoplugin.sdk

import com.goide.GoOsManager
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

private fun File.listDirs(dirName: String): Array<File>? = listFiles { child ->
    child.isDirectory && child.name == dirName
}

private fun checkBin(dir: File): Boolean {
    val binDirCandidates = dir.listDirs("bin")
    return if (!binDirCandidates.isNullOrEmpty()) {
        val binDir = binDirCandidates.first()
        val executableName = (if (GoOsManager.isWindows()) "tinygo.exe" else "tinygo")
        return binDir.listFiles { child ->
            child.isFile && child.canExecute() && child.name == executableName
        }?.isNotEmpty() ?: false
    } else false
}

private fun checkTargets(dir: File): Boolean {
    val targetsDirCandidates = dir.listDirs("targets")
    return if (!targetsDirCandidates.isNullOrEmpty()) {
        val targetsDir = targetsDirCandidates.first()
        return targetsDir.listFiles { child ->
            child.isFile && child.extension == "json"
        }?.isNotEmpty() ?: false
    } else false
}

private fun checkMachinesSources(srcDir: File): Boolean {
    val machinesDirCandidates = srcDir.listDirs("machine")
    return if (!machinesDirCandidates.isNullOrEmpty()) {
        val machinesDir = machinesDirCandidates.first()
        val isBoardOrMachine = { child: String ->
            child.startsWith("machine") || child.startsWith("board")
        }
        return machinesDir.listFiles { child ->
            child.isFile && isBoardOrMachine(child.name) && child.extension == "go"
        }?.isNotEmpty() ?: false
    } else false
}

private fun checkSources(dir: File): Boolean {
    val sourcesDirCandidates = dir.listDirs("src")
    return if (!sourcesDirCandidates.isNullOrEmpty()) {
        val sourcesDir = sourcesDirCandidates.first()
        return checkMachinesSources(sourcesDir)
    } else false
}

fun checkDirectoryForTinyGo(dir: VirtualFile): Boolean {
    val path = dir.canonicalPath
    return if (path != null) {
        val file = File(path)
        return checkDirectoryForTinyGo(file)
    } else false
}

internal fun checkDirectoryForTinyGo(dir: File): Boolean =
    dir.isDirectory && checkBin(dir) && checkTargets(dir) && checkSources(dir)
