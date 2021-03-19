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
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.inspections.inspectionMessage
import org.jetbrains.tinygoplugin.services.UnsupportedPackageProvider

private fun tinyGoLink(packageName: String): String =
    "https://tinygo.org/lang-support/stdlib/#${packageName.replace('/', '-')}"

class ImportInspection : GoInspectionBase() {

    companion object {
        const val IMPORT_INSPECTION_MESSAGE = "inspection.import.message"
        const val IMPORT_QUICK_FIX_FAMILY = "inspection.import.name"

        val UNSUPPORTED_LIBRARY_QUICK_FIX = object : LocalQuickFix {
            override fun getFamilyName(): String = TinyGoBundle.message(IMPORT_QUICK_FIX_FAMILY)

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val element = descriptor.psiElement
                val importDeclaration = element.parent
                val importCount = importDeclaration.children.filter { it is GoImportSpec }.size
                // check if it is declared with
                // import "encoding/json"
                element.delete()
                if (importCount == 1) {
                    importDeclaration.delete()
                }
            }
        }
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
                val target = o.resolve()
                val file = target?.containingFile
                if (file is GoFile) {
                    val importPath = file.getImportPath(false) ?: return
                    if (unsupportedImports.contains(importPath)) {
                        holder.registerProblem(
                            o,
                            inspectionMessage(IMPORT_INSPECTION_MESSAGE, importPath, tinyGoLink(importPath)),
                            ProblemHighlightType.GENERIC_ERROR,
                            UNSUPPORTED_LIBRARY_QUICK_FIX
                        )
                    }
                }
            }

            override fun visitImportSpec(o: GoImportSpec) {
                super.visitImportSpec(o)
                if (o.isCImport || o.isBlank) {
                    return
                }
                val importPath = o.path
                if (unsupportedImports.contains(importPath)) {
                    holder.registerProblem(
                        o,
                        inspectionMessage(IMPORT_INSPECTION_MESSAGE, importPath, tinyGoLink(importPath)),
                        ProblemHighlightType.GENERIC_ERROR,
                        UNSUPPORTED_LIBRARY_QUICK_FIX
                    )
                }
            }
        }
    }
}
