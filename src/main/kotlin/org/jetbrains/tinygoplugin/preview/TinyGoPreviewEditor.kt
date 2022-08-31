package org.jetbrains.tinygoplugin.preview

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.jetbrains.builtInWebServer.BuiltInServerOptions
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class TinyGoPreviewEditor(val project: Project, target: String) : FileEditor, UserDataHolderBase() {
    companion object {
        private val webServerPort = service<BuiltInServerOptions>().effectiveBuiltInServerPort
    }

    private val previewUrl: String =
        "http://localhost:$webServerPort/tinygo-preview?project=${project.basePath}&target=$target"

    private val jcefPanel: JCEFHtmlPanel = JCEFHtmlPanel(previewUrl)

    fun refresh() {
        jcefPanel.loadURL(previewUrl)
    }

    override fun dispose() {
        Disposer.dispose(jcefPanel)
    }

    override fun getComponent(): JComponent = jcefPanel.component

    override fun getPreferredFocusedComponent(): JComponent = component

    override fun getName(): String = "TinyGo Preview"

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
}
