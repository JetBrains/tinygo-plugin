package org.jetbrains.tinygoplugin.heapAllocations.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.tinygoplugin.heapAllocations.TinyGoHeapAlloc
import org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model.RootNode
import org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model.TinyGoHeapAllocsTreeModel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.tree.TreeSelectionModel

private const val TINYGO_HEAP_ALLOC_TOOLWINDOW_ID = "TinyGo: Heap Allocations"

class TinyGoHeapAllocsToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoved(event: ContentManagerEvent) {
                if (toolWindow.contentManager.contentCount == 0) {
                    toolWindow.isAvailable = false
                }
            }
        })
    }

    override fun shouldBeAvailable(project: Project): Boolean = false
}

@Service(Service.Level.PROJECT)
class TinyGoHeapAllocsViewManager(val project: Project) {
    fun updateHeapAllocsList(update: Map<String, Set<TinyGoHeapAlloc>>) {
        val heapAllocsView = TinyGoHeapAllocsWindow(project)
        heapAllocsView.refreshHeapAllocsList(update)
        activateToolWindow(heapAllocsView)
    }

    private fun activateToolWindow(heapAllocsView: TinyGoHeapAllocsWindow) {
        invokeLater {
            val toolWindow: ToolWindow = project.service<ToolWindowManager>()
                .getToolWindow(TINYGO_HEAP_ALLOC_TOOLWINDOW_ID)
                ?: return@invokeLater
            val contentManager = toolWindow.contentManager

            contentManager.removeAllContents(true)
            val contentTab: Content = contentManager.factory.createContent(heapAllocsView, project.name, false)
            contentManager.addContent(contentTab)

            toolWindow.isAvailable = true
            toolWindow.activate(null, true)
        }
    }
}

class TinyGoHeapAllocsWindow(val project: Project) : JPanel(BorderLayout()), Disposable {
    private val treeModel = TinyGoHeapAllocsTreeModel(this)
    private val tree: Tree = Tree(treeModel)

    init {
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.isRootVisible = false
        tree.setupGoToSourceOnDoubleClick()

        add(BorderLayout.CENTER, ScrollPaneFactory.createScrollPane(tree, true))
    }

    fun refreshHeapAllocsList(update: Map<String, Set<TinyGoHeapAlloc>>) {
        treeModel.root = RootNode(project, update)
        tree.expandAllNodes()
    }

    @Suppress("EmptyFunctionBlock")
    override fun dispose() {
    }
}
