package org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model

import com.intellij.openapi.util.text.StringUtil

class HeapAllocNodeComparator : Comparator<Node> {
    @Suppress("ReturnCount")
    override fun compare(node1: Node?, node2: Node?): Int {
        if (node1 === node2) return 0
        if (node1 == null) return +1
        if (node2 == null) return -1
        if (node1 is HeapAllocNode && node2 is HeapAllocNode) return compare(node1, node2)
        return StringUtil.naturalCompare(node1.name, node2.name)
    }

    private fun compare(node1: HeapAllocNode, node2: HeapAllocNode): Int {
        val result = StringUtil.naturalCompare(node1.heapAlloc.file.name, node1.heapAlloc.file.name)
        return if (result != 0) result else comparePosition(node1, node2)
    }

    private fun comparePosition(node1: HeapAllocNode, node2: HeapAllocNode): Int {
        val result = node1.heapAlloc.line.compareTo(node2.heapAlloc.line)
        return if (result != 0) result else node1.heapAlloc.column.compareTo(node2.heapAlloc.column)
    }
}
