package org.jetbrains.tinygoplugin.heapAllocations

import com.goide.GoFileType
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.containers.orNull
import com.intellij.util.io.isDirectory
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.runconfig.isMainGoFile
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkVersion
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured
import org.jetbrains.tinygoplugin.services.TinyGoExecutable
import org.jetbrains.tinygoplugin.services.tinyGoArguments
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer

@Suppress("MagicNumber")
val TINYGO_PRINT_HEAP_ALLOCS_MIN_VER = TinyGoSdkVersion(0, 18, 0)

data class TinyGoHeapAlloc(
    val file: VirtualFile,
    val line: Int,
    val column: Int,
    val reason: String
) {
    override fun toString(): String = "${file.canonicalPath}:$line:$column"
}

interface HeapAllocsWatcher : AnActionListener {
    fun refreshHeapAllocsList(blameOutdatedVersion: Boolean = true)
}

@Service
class TinyGoHeapAllocsSupplier private constructor() {
    companion object {
        fun getInstance(): TinyGoHeapAllocsSupplier = service()

        private fun supplyMainFile(project: Project): Path? = Files.walk(Path.of(project.basePath!!)).filter {
            if (it.isDirectory() || it.toFile().extension != GoFileType.DEFAULT_EXTENSION) return@filter false
            val file = VfsUtil.findFile(it, false) ?: return@filter false
            val psiFile = PsiUtilBase.getPsiFile(project, file)
            isMainGoFile(psiFile)
        }.findFirst().orNull()

        const val TINYGO_PRINT_HEAP_ALLOCS_OUTDATED_MESSAGE =
            "notifications.tinygoSDK.detection.heapAllocs.outdatedCompiler"
    }

    val listeners: MutableSet<HeapAllocsWatcher> = mutableSetOf()

    fun notifyHeapAllocWatchers() {
        listeners.forEach {
            it.refreshHeapAllocsList()
        }
    }

    fun supplyHeapAllocs(
        project: Project,
        blameOutdatedVersion: Boolean = true,
        consumer: Consumer<Map<String, Set<TinyGoHeapAlloc>>>,
    ) {
        val result = mutableMapOf<String, MutableSet<TinyGoHeapAlloc>>()

        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        val heapAllocRegex = Regex("(/.+/+.+.go):([0-9]+):([0-9]+): (.+)")

        val sdk = tinyGoSettings.sdk
        val sdkRoot = sdk.sdkRoot?.canonicalPath ?: return
        val mainFile = supplyMainFile(project) ?: return
        val extractionParameters = listOf("build", "-o", "temp.out", "-print-allocs=.") +
            tinyGoArguments(tinyGoSettings) +
            listOf("${mainFile.toAbsolutePath()}")

        if (blameOutdatedVersion && sdk.sdkVersion.isLessThan(TINYGO_PRINT_HEAP_ALLOCS_MIN_VER)) {
            notifyTinyGoNotConfigured(
                project,
                TinyGoBundle.message(
                    TINYGO_PRINT_HEAP_ALLOCS_OUTDATED_MESSAGE,
                    TINYGO_PRINT_HEAP_ALLOCS_MIN_VER.toString()
                )
            )
            consumer.accept(result)
            return
        }
        TinyGoExecutable(project).execute(sdkRoot, extractionParameters) { _, output ->
            val matches = heapAllocRegex.findAll(output)
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
            consumer.accept(result)
        }
    }
}
