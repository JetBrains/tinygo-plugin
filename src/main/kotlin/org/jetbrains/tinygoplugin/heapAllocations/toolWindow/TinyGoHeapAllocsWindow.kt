package org.jetbrains.tinygoplugin.heapAllocations.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.heapAllocations.HeapAllocsWatcher
import org.jetbrains.tinygoplugin.heapAllocations.TinyGoHeapAllocsSupplier
import org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model.RootNode
import org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model.TinyGoHeapAllocsTreeModel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.tree.TreeSelectionModel

class TinyGoHeapAllocationsWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = TinyGoHeapAllocsWindow(project)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content: Content = contentFactory.createContent(myToolWindow.panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun isApplicable(project: Project): Boolean = TinyGoConfiguration.getInstance(project).enabled
}

class TinyGoHeapAllocsWindow(val project: Project) : Disposable, HeapAllocsWatcher {
    companion object {
        private const val TOOLBAR_ACTION_GROUP_ID = "HeapAllocs.ToolWindow.Toolbar"
    }

    private val treeModel = TinyGoHeapAllocsTreeModel(this)
    val tree: Tree = Tree(treeModel)
    val panel = JPanel(BorderLayout())

    init {
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.isRootVisible = false
        tree.setupGoToSourceOnDoubleClick()

        val myToolbar = getToolbar()
        myToolbar.setOrientation(SwingConstants.HORIZONTAL)
        myToolbar.setTargetComponent(tree)
        myToolbar.component.isVisible = true

        TinyGoHeapAllocsSupplier.getInstance().listeners.add(this)
        refreshHeapAllocsList(false)

        UIUtil.addBorder(myToolbar.component, CustomLineBorder(JBUI.insetsBottom(1)))
        panel.add(BorderLayout.CENTER, ScrollPaneFactory.createScrollPane(tree, true))
        panel.add(BorderLayout.NORTH, myToolbar.component)
    }

    private fun getToolbar(): ActionToolbar {
        val group = ActionManager.getInstance().getAction(TOOLBAR_ACTION_GROUP_ID) as ActionGroup
        return ActionManager.getInstance().createActionToolbar(javaClass.name, group, false)
    }

    private fun expandAllNodes() {
        for (i in 0..treeModel.root?.getChildren()?.size!!) {
            tree.expandRow(i)
        }
    }

    override fun refreshHeapAllocsList(blameOutdatedVersion: Boolean) {
        FileDocumentManager.getInstance().saveAllDocuments()
        TinyGoHeapAllocsSupplier.getInstance().supplyHeapAllocs(project, blameOutdatedVersion) {
            treeModel.root = RootNode(project, it)
            expandAllNodes()
        }
    }

    override fun dispose() {
        TinyGoHeapAllocsSupplier.getInstance().listeners.remove(this)
    }
}
