package org.jetbrains.tinygoplugin.preview

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager

class TinyGoPreviewManager(private val editor: TinyGoFileEditorWithPreview) {
    private val preview = editor.previewEditor as TinyGoPreviewEditor

    fun compileWasm(onFinish: () -> Unit) {
        val file = editor.file ?: return
        val document = service<FileDocumentManager>().getCachedDocument(file) ?: return
        invokeLater {
            service<FileDocumentManager>().saveDocument(document)
            preview.project.service<TinyGoPreviewWasmService>()
                .compileWasm(file) { onFinish.invoke() }
        }
    }

    fun refreshJcef() {
        preview.refresh()
    }

    fun disposeWasm() {
        preview.project.service<TinyGoPreviewWasmService>().disposeWasm()
    }
}
