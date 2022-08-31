package org.jetbrains.tinygoplugin.preview

import com.intellij.openapi.components.service

class TinyGoPreviewManager(private val editor: TinyGoFileEditorWithPreview) {
    private val preview = editor.previewEditor as TinyGoPreviewEditor

    fun compileWasm(onFinish: () -> Unit) {
        val scratchFile = editor.file ?: return
        preview.project.service<TinyGoPreviewWasmService>()
            .compileWasm(scratchFile) {
                onFinish.invoke()
            }
    }

    fun refreshJcef() {
        preview.refresh()
    }

    fun disposeWasm() {
        preview.project.service<TinyGoPreviewWasmService>().disposeWasm()
    }
}
