package org.jetbrains.tinygoplugin.heapAllocations.toolWindow

import com.intellij.ide.impl.ContentManagerWatcher
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.heapAllocations.TinyGoHeapAlloc
import org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model.RootNode
import org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model.TinyGoHeapAllocsTreeModel
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.tree.TreeSelectionModel

private const val TINYGO_HEAP_ALLOC_TOOLWINDOW_ID = "HeapAlloc"
private const val TINYGO_HEAP_ALLOC_TOOLWINDOW_TITLE = "heapAllocs.view.title"

@Service
class TinyGoHeapAllocsViewManager(val project: Project) {
    private lateinit var contentManager: ContentManager

    init {
        ApplicationManager.getApplication().invokeLater {
            val registerToolWindowTask: RegisterToolWindowTask =
                RegisterToolWindowTask.closableSecondary(
                    TINYGO_HEAP_ALLOC_TOOLWINDOW_ID,
                    TinyGoBundle.messagePointer(TINYGO_HEAP_ALLOC_TOOLWINDOW_TITLE),
                    TinyGoPluginIcons.TinyGoLibraryIcon,
                    ToolWindowAnchor.RIGHT
                )

            val toolWindow: ToolWindow = project.service<ToolWindowManager>().registerToolWindow(registerToolWindowTask)
            contentManager = toolWindow.contentManager
            ContentManagerWatcher.watchContentManager(toolWindow, contentManager)
        }
    }

    fun updateHeapAllocsList(update: Map<String, Set<TinyGoHeapAlloc>>) {
        val heapAllocsView = TinyGoHeapAllocsWindow(project)
        heapAllocsView.refreshHeapAllocsList(update)
        activateToolWindow(heapAllocsView)
    }

    private fun activateToolWindow(heapAllocsView: TinyGoHeapAllocsWindow) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindow: ToolWindow = project.service<ToolWindowManager>()
                .getToolWindow(TINYGO_HEAP_ALLOC_TOOLWINDOW_ID)
                ?: return@invokeLater

            contentManager.removeAllContents(true)

            val contentTab: Content = contentManager.factory.createContent(heapAllocsView, project.name, false)
            contentManager.addContent(contentTab)

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
