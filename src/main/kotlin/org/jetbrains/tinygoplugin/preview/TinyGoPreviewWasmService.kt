package org.jetbrains.tinygoplugin.preview

import com.goide.util.GoExecutor
import com.goide.util.GoUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference

@Service
internal class TinyGoPreviewWasmService(val project: Project) {
    private val compilationStatus: ConcurrentMap<String, AtomicReference<CompilationStatus>> = ConcurrentHashMap()

    private fun getOutputFile(scratchFile: String): String =
        GoUtil.getGoLandTempDirectory().toString() +
            "tinygo-temp-output-${Integer.toHexString(scratchFile.hashCode())}.wasm"

    fun compileWasm(scratchFile: VirtualFile, onFinish: () -> Unit) {
        compilationStatus[scratchFile.path] = AtomicReference(CompilationStatus.InProgress)

        val tinyGoConfiguration = TinyGoConfiguration.getInstance(project)
        val arguments = listOf(
            "build",
            "-tags=${tinyGoConfiguration.targetPlatform}",
            "-opt=1",
            "-no-debug",
            "-o=${getOutputFile(scratchFile.path)}",
            scratchFile.canonicalPath!!
        )
        val executor = GoExecutor.`in`(project, null)
            .withExePath(tinyGoConfiguration.sdk.executable?.canonicalPath)
            .withParameters(arguments)

        executor.executeWithProgress {
            if (it.status.ordinal == 0) {
                compilationStatus[scratchFile.path]!!.set(CompilationStatus.OK)
                onFinish.invoke()
            } else {
                compilationStatus[scratchFile.path]!!.set(CompilationStatus.Failed)
            }
        }
    }

    companion object {
        private enum class CompilationStatus {
            OK,
            Failed,
            InProgress
        }

        const val COMPILATION_WAIT_INTERVAL: Long = 100
    }

    fun getWasm(scratchFile: String): ByteArray? {
        while (compilationStatus[scratchFile]?.get() == CompilationStatus.InProgress) {
            Thread.sleep(COMPILATION_WAIT_INTERVAL)
        }
        return if (compilationStatus[scratchFile]?.get() == CompilationStatus.OK) {
            File(getOutputFile(scratchFile)).readBytes()
        } else null
    }

    fun disposeWasm() {
        compilationStatus.keys.forEach {
            Files.deleteIfExists(File(it).toPath())
        }
    }
}
