package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoInspectionMessage
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoConditionalExpr
import com.goide.psi.GoInterfaceType
import com.goide.psi.GoVisitor
import com.goide.psi.impl.GoNamedElementImpl
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.psi.PsiElement

class InterfaceComparisonVisitor(
    private val holder: GoProblemsHolder,
) : GoVisitor() {
    private fun isInterface(o: PsiElement): Boolean {
        val reference = o.reference?.resolve() ?: return false
        val goType = (reference as GoNamedElementImpl<*>).getGoUnderlyingType(null)
        return goType is GoInterfaceType
    }

    override fun visitConditionalExpr(o: GoConditionalExpr) {
        super.visitConditionalExpr(o)
        if (o.eq != null || o.notEq != null) {
            val children = o.children
            if (children.size != 2) {
                return
            }
            val interfaceComparison = children.all { isInterface(it) }
            if (interfaceComparison) {
                holder.registerProblem(
                    o,
                    object : GoInspectionMessage {
                        override fun getTemplate(): String = ""
                        override fun toString(): String = "<html>Two interfaces should not be compared." +
                            "<p>TinyGo does not support interface comparison and it will always return false. " +
                            "Only comparison with nil works.</p> </html>"
                    }
                )
            }
        }
    }
}

class InterfaceInspection : GoInspectionBase() {
    companion object {}

    override fun buildGoVisitor(holder: GoProblemsHolder, session: LocalInspectionToolSession): GoVisitor {
        return InterfaceComparisonVisitor(holder)
    }
}
