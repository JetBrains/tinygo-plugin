package org.jetbrains.tinygoplugin.sdk

import com.goide.GoOsManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class TinyGoSdkUtil {
    companion object {
        private fun checkDirectoryForTinyGo(dir: File) : Boolean {
            if (dir.isDirectory) {
                val binDir = dir.listFiles { child -> child.isDirectory && child.name.endsWith("bin") }?.first()
                return binDir?.listFiles { child ->
                    child.isFile && child.canExecute() && child.name.endsWith("tinygo") // TODO: research if other criteria possible
                }?.isNotEmpty() ?: false
            }
            return false
        }

        fun suggestSdkDirectory() : VirtualFile? {
            if (GoOsManager.isMac()) { // TODO: implement search in PATH
                val macPorts = "/opt/local/lib/tinygo"
                val homeBrew = "/usr/local/Cellar/tinygo"
                val file = FileUtil.findFirstThatExist(macPorts, homeBrew)
                if (file != null) {
                    val tinyGoSdkDirectories = file.canonicalFile.listFiles { child -> checkDirectoryForTinyGo(child) }?.first()
                    if (tinyGoSdkDirectories != null) {
                        return LocalFileSystem.getInstance().findFileByIoFile(tinyGoSdkDirectories)
                    }
                }
            }
            return null
        } //TODO: implement search on other platforms
    }
}