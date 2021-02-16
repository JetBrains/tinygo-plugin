package org.jetbrains.tinygoplugin.sdk

import com.goide.GoOsManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class TinyGoSdkUtil private constructor() {
    companion object {
        fun checkDirectoryForTinyGo(dir: VirtualFile): Boolean {
            val path = dir.canonicalPath
            if (path != null) {
                val file = File(path)
                return checkDirectoryForTinyGo(file)
            }
            return false
        }

        private fun checkDirectoryForTinyGo(dir: File): Boolean {
            if (dir.isDirectory) {
                val binDirCandidates = dir.listFiles { child -> child.isDirectory && child.name.endsWith("bin") }
                    if (binDirCandidates != null) {
                        if (binDirCandidates.isNotEmpty()) {
                            val binDir = binDirCandidates.first()
                            // research if other criteria possible
                            return binDir?.listFiles { child ->
                                child.isFile && child.canExecute() && child.name.endsWith("tinygo")
                            }?.isNotEmpty() ?: false
                        }
                    }
            }
            return false
        }

        fun suggestSdkDirectoryStr(): String = suggestSdkDirectory()?.canonicalPath ?: ""

        private fun suggestSdkDirectory(): VirtualFile? {
            // TODO: implement search on other platforms
            if (GoOsManager.isMac()) {
                val macPorts = "/opt/local/lib/tinygo"
                val homeBrew = "/usr/local/Cellar/tinygo" // TODO: implement search in PATH
                val file = FileUtil.findFirstThatExist(macPorts, homeBrew)
                if (file != null) {
                    val tinyGoSdkDirectories = file.canonicalFile.listFiles {
                            child -> checkDirectoryForTinyGo(child)
                    }?.first()
                    if (tinyGoSdkDirectories != null) {
                        return LocalFileSystem.getInstance().findFileByIoFile(tinyGoSdkDirectories)
                    }
                }
            }
            return null
        }
    }
}
