package org.jetbrains.tinygoplugin.heapAllocations.toolWindow

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.tinygoplugin.heapAllocations.TinyGoHeapAllocsSupplier

class RefreshHeapAllocsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        TinyGoHeapAllocsSupplier.getInstance().notifyHeapAllocWatchers()
    }
}
