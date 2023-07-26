package org.jetbrains.tinygoplugin.heapAllocations.toolWindow

import com.intellij.openapi.application.ModalityState
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.OpenSourceUtil
import org.jetbrains.tinygoplugin.heapAllocations.toolWindow.model.HeapAllocNode
import java.awt.event.MouseEvent

internal fun Tree.setupGoToSourceOnDoubleClick() {
    object : DoubleClickListener() {
        override fun onDoubleClick(e: MouseEvent): Boolean {
            if (ModalityState.current().dominates(ModalityState.nonModal())) return false
            if (getPathForLocation(e.x, e.y) == null) return false
            val element = getPathForLocation(e.x, e.y)?.lastPathComponent
            if (element is HeapAllocNode) {
                OpenSourceUtil.navigate(true, element.getNavigatable())
            }
            return true
        }
    }.installOn(this)
}

internal fun Tree.expandAllNodes() {
    for (i in 0..rowCount) {
        expandRow(i)
    }
}
