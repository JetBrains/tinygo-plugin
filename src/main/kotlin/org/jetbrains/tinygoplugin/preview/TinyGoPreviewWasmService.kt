package org.jetbrains.tinygoplugin.preview

import com.goide.util.GoExecutor
import com.goide.util.GoUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

@Service
internal class TinyGoPreviewWasmService(val project: Project) {
    private val compiled = AtomicBoolean()

    private val outputFile: String =
        GoUtil.getGoLandTempDirectory().toString() + "tinygo-temp-output-${project.locationHash}.wasm"

    fun compileWasm(scratchFile: VirtualFile, onFinish: () -> Unit) {
        compiled.set(false)

        val tinyGoConfiguration = TinyGoConfiguration.getInstance(project)
        val arguments = mutableListOf(
            "build",
            "-tags=${tinyGoConfiguration.targetPlatform}",
            "-opt=1",
            "-no-debug",
            "-o=$outputFile",
            scratchFile.canonicalPath!!
        )
        val executor = GoExecutor.`in`(project, null)
            .withExePath(tinyGoConfiguration.sdk.executable?.canonicalPath)
            .withParameters(arguments)

        executor.executeWithProgress {
            if (it.status.ordinal == 0) {
                compiled.set(true)
                onFinish.invoke()
            }
        }
    }

    companion object {
        const val COMPILATION_WAIT_INTERVAL: Long = 100
    }

    fun getWasm(): ByteArray {
        while (!compiled.get()) {
            Thread.sleep(COMPILATION_WAIT_INTERVAL)
        }
        return File(outputFile).readBytes()
    }

    fun disposeWasm() {
        Files.deleteIfExists(File(outputFile).toPath())
    }
}
