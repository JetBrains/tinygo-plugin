package org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model

import com.goide.GoIcons
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.tree.LeafState
import com.intellij.ui.tree.TreePathUtil
import org.jetbrains.tinygoplugin.heapAllocations.TinyGoHeapAlloc
import java.util.Objects

@Suppress("TooManyFunctions")
abstract class Node : PresentableNodeDescriptor<Node?>, LeafState.Supplier {
    protected constructor(project: Project) : super(project, null)
    protected constructor(parent: Node) : super(parent.project, parent)

    open val descriptor: OpenFileDescriptor? = null

    protected abstract fun update(project: Project, presentation: PresentationData)

    abstract override fun getName(): String

    override fun toString() = name

    open fun getChildren(): Collection<Node> = emptyList()

    open fun getVirtualFile(): VirtualFile? = null

    open fun getNavigatable(): Navigatable? = descriptor

    override fun getElement() = this

    override fun update(presentation: PresentationData) {
        if (myProject == null || myProject.isDisposed) return
        update(myProject, presentation)
    }

    fun getPath() = TreePathUtil.pathToCustomNode(this) { node: Node? -> node?.getParent(Node::class.java) }!!

    fun <T> getParent(type: Class<T>): T? {
        val parent = parentDescriptor ?: return null
        @Suppress("UNCHECKED_CAST")
        if (type.isInstance(parent)) return parent as T
        throw IllegalStateException("unexpected node " + parent.javaClass)
    }

    fun <T> findAncestor(type: Class<T>): T? {
        var parent = parentDescriptor
        while (parent != null) {
            @Suppress("UNCHECKED_CAST")
            if (type.isInstance(parent)) return parent as T
            parent = parent.parentDescriptor
        }
        return null
    }
}

class RootNode(project: Project, private val heapAllocClusters: Map<String, Set<TinyGoHeapAlloc>>) :
    Node(project),
    Disposable {
    override fun dispose() = Unit

    override fun getLeafState() = LeafState.NEVER

    override fun getName() = ""

    override fun update(project: Project, presentation: PresentationData) {
        presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun getChildren(): Collection<Node> {
        val children = mutableListOf<Node>()
        heapAllocClusters.forEach { (dir, allocs) ->
            children += DirNode(this, dir, allocs)
        }
        return children
    }
}

class DirNode(parent: Node, val dir: String, val heapAllocs: Set<TinyGoHeapAlloc>) : Node(parent) {
    companion object {
        fun supplyDirPresentationName(oldName: String): String {
            return if (oldName.contains(System.getProperty("user.home"))) {
                oldName.replace(System.getProperty("user.home"), "~")
            } else oldName
        }
    }

    override fun update(project: Project, presentation: PresentationData) {
        presentation.addText(supplyDirPresentationName(name), SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.setIcon(AllIcons.Nodes.Folder)
    }

    override fun getName(): String = dir

    override fun getLeafState(): LeafState = LeafState.ALWAYS

    override fun getChildren(): Collection<Node> {
        val children = mutableListOf<Node>()
        heapAllocs.forEach { children += HeapAllocNode(this, it) }
        return children
    }
}

class HeapAllocNode(parent: Node, val heapAlloc: TinyGoHeapAlloc) : Node(parent) {
    override val descriptor
        get() = project?.let { OpenFileDescriptor(it, heapAlloc.file, heapAlloc.line - 1, heapAlloc.column) }

    override fun getLeafState() = LeafState.ALWAYS

    override fun getName() = heapAlloc.toString()

    override fun getVirtualFile() = heapAlloc.file

    override fun getNavigatable() = descriptor

    override fun update(project: Project, presentation: PresentationData) {
        presentation.addText(heapAlloc.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.setIcon(GoIcons.ICON)
        presentation.tooltip = heapAlloc.reason
        if (heapAlloc.line >= 0) presentation.addText(" :${heapAlloc.line}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        presentation.addText(" ${heapAlloc.reason}", SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES)
    }

    override fun hashCode() = Objects.hash(project, heapAlloc)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this.javaClass != other?.javaClass) return false
        val that = other as? HeapAllocNode ?: return false
        return that.project == project && that.heapAlloc == heapAlloc
    }
}
