package org.jetbrains.tinygoplugin.preview

import com.goide.GoFileType
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

class TinyGoPreviewFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (!JBCefApp.isSupported()) return false
        val isGoFile = FileTypeRegistry.getInstance().isFileOfType(file, GoFileType.INSTANCE)
        val tinyGoEnabled = TinyGoConfiguration.getInstance(project).enabled
        val isScratch = ScratchUtil.isScratch(file)
        return isGoFile && tinyGoEnabled && isScratch
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        TinyGoFileEditorWithPreview.create(project, file, TinyGoConfiguration.getInstance(project).targetPlatform)

    override fun getEditorTypeId(): String = "TinyGoPreviewFileEditor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

class TinyGoFileEditorWithPreview private constructor(
    editor: TextEditor,
    preview: TinyGoPreviewEditor,
) : TextEditorWithPreview(editor, preview), TextEditor {
    companion object {
        fun create(project: Project, file: VirtualFile, target: String): FileEditor {
            val textEditorProvider = TextEditorProvider.getInstance()
            val editor = textEditorProvider.createEditor(project, file)

            return (editor as? TextEditor)?.let {
                TinyGoFileEditorWithPreview(it, TinyGoPreviewEditor(project, editor.file.path, target))
            } ?: editor
        }
    }

    private val manager = TinyGoPreviewManager(this)

    private val toolbarGroup = DefaultActionGroup().apply {
        add(TinyGoPreviewRunAction(manager))
    }

    private val toolbar = service<ActionManager>()
        .createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true)

    override fun createToolbar(): ActionToolbar = toolbar

    override fun createViewActionGroup(): ActionGroup = DefaultActionGroup(showEditorAndPreviewAction)

    override fun dispose() {
        manager.disposeWasm()
        super.dispose()
    }
}

const val TINYGO_PREVIEW_TITLE = "preview.run.title"
const val TINYGO_PREVIEW_DESCRIPTION = "preview.run.description"

class TinyGoPreviewRunAction(private val manager: TinyGoPreviewManager) :
    AnAction(
        TinyGoBundle.message(TINYGO_PREVIEW_TITLE),
        TinyGoBundle.message(TINYGO_PREVIEW_DESCRIPTION),
        AllIcons.Actions.Execute
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        manager.compileWasm {
            manager.refreshJcef()
        }
    }
}
