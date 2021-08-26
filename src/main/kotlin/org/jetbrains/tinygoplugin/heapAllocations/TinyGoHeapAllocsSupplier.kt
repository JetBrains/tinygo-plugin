package org.jetbrains.tinygoplugin.heapAllocations

import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.services.TinyGoExecutable
import org.jetbrains.tinygoplugin.services.tinyGoArguments
import java.io.File
import java.util.function.Consumer

data class TinyGoHeapAlloc(
    val file: VirtualFile,
    val line: Int,
    val column: Int,
    val reason: String
) {
    override fun toString(): String {
        return "${file.canonicalPath}:$line:$column"
    }
}

interface HeapAllocsWatcher : AnActionListener {
    fun refreshHeapAllocsList()
}

@Service
class TinyGoHeapAllocsSupplier private constructor() {
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoHeapAllocsSupplier::class.java)

        fun getInstance(): TinyGoHeapAllocsSupplier = service()
    }

    val listeners: MutableSet<HeapAllocsWatcher> = mutableSetOf()

    fun notifyHeapAllocWatchers() {
        listeners.forEach {
            it.refreshHeapAllocsList()
        }
    }

    fun supplyHeapAllocs(project: Project, consumer: Consumer<Map<String, Set<TinyGoHeapAlloc>>>) {
        val result = mutableMapOf<String, MutableSet<TinyGoHeapAlloc>>()

        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        val heapAllocRegex = Regex("(/.+/+.+.go):([0-9]+):([0-9]+): (.+)")

        val sdkRoot = tinyGoSettings.sdk.sdkRoot?.canonicalPath ?: return
        val extractionParameters = listOf("build", "-o", "temp.out", "-print-allocs=.") +
            tinyGoArguments(tinyGoSettings) +
            listOf("${project.basePath}/main.go")

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
