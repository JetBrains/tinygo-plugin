package org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.tree.BaseTreeModel
import com.intellij.util.asSafely
import com.intellij.util.concurrency.Invoker
import com.intellij.util.concurrency.InvokerSupplier
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import javax.swing.tree.TreePath

class TinyGoHeapAllocsTreeModel(parent: Disposable) : BaseTreeModel<Node?>(), InvokerSupplier {
    private val invoker = Invoker.forBackgroundThreadWithReadAction(this)
    private val root = AtomicReference<RootNode>()
    private val comparator = AtomicReference<Comparator<Node>?>(HeapAllocNodeComparator())
    override fun dispose() {
        super.dispose()
        setRoot(null)
    }

    override fun getInvoker(): Invoker {
        return invoker
    }

    fun isRoot(root: RootNode): Boolean {
        return root === this.root.get()
    }

    override fun getRoot(): RootNode? {
        val root = this.root.get()
        root?.update()
        return root
    }

    fun setRoot(root: RootNode?) {
        val old = this.root.getAndSet(root)
        if (old !== root && old != null) Disposer.dispose(old)
        structureChanged(null)
    }

    override fun getChildren(`object`: Any): List<Node> {
        val node = `object`.asSafely<Node>() ?: return emptyList()
        val children = node.getChildren()
        if (children.isEmpty()) return emptyList()
        assert(null != comparator.get()) { "set comparator before" }
        node.update()
        children.forEach { it.update() }
        return children.stream().sorted(comparator.get()).collect(Collectors.toList())
    }

    fun setComparator(comparator: Comparator<Node>) {
        if (comparator != this.comparator.getAndSet(comparator)) structureChanged(null)
    }

    fun structureChanged(path: TreePath?) {
        treeStructureChanged(path, null, null)
    }

    fun nodeChanged(path: TreePath) {
        treeNodesChanged(path, null, null)
    }

    init {
        Disposer.register(parent, this)
    }
}
