package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoConditionalExpr
import com.goide.psi.GoNamedElement
import com.goide.psi.GoVisitor
import com.goide.psi.impl.GoTypeUtil
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.psi.PsiElement

class InterfaceInspection : GoInspectionBase() {
    companion object {
        private const val INTERFACE_INSPECTION_ERROR_MESSAGE = "<html>Two interfaces should not be compared." +
            "<p>TinyGo does not support interface comparison and it will always return false. " +
            "Only comparison with nil works.</p> </html>"
    }

    override fun buildGoVisitor(holder: GoProblemsHolder, session: LocalInspectionToolSession): GoVisitor {
        return object : GoVisitor() {
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
                            TinyGoInspectionMessage(INTERFACE_INSPECTION_ERROR_MESSAGE)
                        )
                    }
                }
            }

            @Suppress("ReturnCount")
            private fun isInterface(o: PsiElement): Boolean {
                val variableDeclaration = o.reference?.resolve() ?: return false
                if (variableDeclaration is GoNamedElement) {
                    val goType = variableDeclaration.getGoType(null) ?: return false
                    val typeSpec = GoTypeUtil.findTypeSpec(goType, null) ?: return false
                    return GoTypeUtil.isInterface(typeSpec)
                }
                return false
            }
        }
    }
}
