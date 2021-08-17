package org.jetbrains.tinygoplugin.inspections.libraries

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoFile
import com.goide.psi.GoImportSpec
import com.goide.psi.GoReferenceExpression
import com.goide.psi.GoVisitor
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.inspections.inspectionMessage
import org.jetbrains.tinygoplugin.services.UnsupportedPackageProvider

private fun tinyGoLink(packageName: String): String =
    "https://tinygo.org/lang-support/stdlib/#${packageName.replace('/', '-')}"

open class TinyGoImportInspection : GoInspectionBase() {

    companion object {
        private const val IMPORT_INSPECTION_MESSAGE = "inspection.import.unsupported.message"
        private const val IMPORT_QUICK_FIX_FAMILY = "inspection.import.unsupported.name"

        val UNSUPPORTED_LIBRARY_QUICK_FIX = object : LocalQuickFix {
            override fun getFamilyName(): String = TinyGoBundle.message(IMPORT_QUICK_FIX_FAMILY)

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val element = descriptor.psiElement
                val importDeclaration = element.parent
                val importCount = importDeclaration.children.filterIsInstance<GoImportSpec>().size
                // check if it is declared with
                // import "encoding/json"
                element.delete()
                if (importCount == 1) {
                    importDeclaration.delete()
                }
            }
        }
    }

    protected fun GoProblemsHolder.registerImportProblem(o: PsiElement, message: String, importPath: String = "") {
        val concatenatedMessage = if (importPath.isEmpty()) inspectionMessage(message)
        else inspectionMessage(message, importPath, tinyGoLink(importPath))

        registerProblem(
            o,
            concatenatedMessage,
            ProblemHighlightType.GENERIC_ERROR,
            UNSUPPORTED_LIBRARY_QUICK_FIX
        )
    }

    override fun buildGoVisitor(
        holder: GoProblemsHolder,
        locationInspection: LocalInspectionToolSession,
    ): GoVisitor {
        val project = holder.manager.project
        val unsupportedImports = project.service<UnsupportedPackageProvider>().unsupportedLibraries()
        return object : GoVisitor() {
            override fun visitReferenceExpression(o: GoReferenceExpression) {
                super.visitReferenceExpression(o)
                if (!TinyGoConfiguration.getInstance(o.project).enabled) {
                    return
                }
                val target = o.resolve()
                val file = target?.containingFile
                if (file is GoFile) {
                    val importPath = file.getImportPath(false) ?: return
                    if (unsupportedImports.contains(importPath)) {
                        holder.registerImportProblem(o, IMPORT_INSPECTION_MESSAGE, importPath)
                    }
                }
            }

            override fun visitImportSpec(o: GoImportSpec) {
                super.visitImportSpec(o)
                if (o.isCImport || o.isBlank || !TinyGoConfiguration.getInstance(o.project).enabled) {
                    return
                }
                val importPath = o.path
                if (unsupportedImports.contains(importPath)) {
                    holder.registerImportProblem(o, IMPORT_INSPECTION_MESSAGE, importPath)
                }
            }
        }
    }
}
