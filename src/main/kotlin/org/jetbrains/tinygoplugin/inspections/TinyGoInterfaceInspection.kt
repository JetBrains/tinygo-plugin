package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoConditionalExpr
import com.goide.psi.GoInterfaceType
import com.goide.psi.GoNamedElement
import com.goide.psi.GoVisitor
import com.goide.psi.impl.GoTypeUtil
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.psi.PsiElement
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration

class TinyGoInterfaceInspection : GoInspectionBase() {
    companion object {
        const val INTERFACE_INSPECTION_MESSAGE_KEY = "inspection.interface.message"
    }

    override fun buildGoVisitor(holder: GoProblemsHolder, session: LocalInspectionToolSession): GoVisitor =
        object : GoVisitor() {
            override fun visitConditionalExpr(conditionalExpr: GoConditionalExpr) {
                val tinyGoConfiguration = conditionalExpr.project.tinyGoConfiguration()
                if (!tinyGoConfiguration.enabled) {
                    return
                }

                super.visitConditionalExpr(conditionalExpr)
                if (conditionalExpr.eq != null || conditionalExpr.notEq != null) {
                    val arguments = conditionalExpr.children
                    if (arguments.size != 2) {
                        return
                    }
                    val interfaceComparison = arguments.all { isInterface(it) }
                    if (interfaceComparison) {
                        holder.registerProblem(
                            conditionalExpr,
                            inspectionMessage(INTERFACE_INSPECTION_MESSAGE_KEY)
                        )
                    }
                }
            }
        }
}

@Suppress("ReturnCount")
private fun isInterface(element: PsiElement): Boolean {
    val variableDeclaration = element.reference?.resolve() ?: return false
    if (variableDeclaration !is GoNamedElement) {
        return false
    }
    val goType = variableDeclaration.getGoType(null) ?: return false
    // check if variable is declared with
    // var a interface{}
    if (goType is GoInterfaceType) {
        return true
    }
    val typeSpec = GoTypeUtil.findTypeSpec(goType, null) ?: return false
    return GoTypeUtil.isInterface(typeSpec)
}
