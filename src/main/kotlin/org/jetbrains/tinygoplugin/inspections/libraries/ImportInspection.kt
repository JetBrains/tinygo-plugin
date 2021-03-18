package org.jetbrains.tinygoplugin.inspections.libraries

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoInspectionMessage
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
import org.jetbrains.tinygoplugin.services.UnsupportedPackageProvider

class UnsupportedLibraryMessage(private val packageName: String) : GoInspectionMessage {
    override fun toString(): String {
        val tinyGoLink = "https://tinygo.org/lang-support/stdlib/#${packageName.replace('/', '-')}"
        val tinyGoLinkHttp = """<a href="$tinyGoLink">$packageName</a>"""
        return "<html>$packageName is not supported by TinyGo. For details see $tinyGoLinkHttp</html>"
    }
}

class ImportInspection : GoInspectionBase() {

    companion object {
        val UNSUPPORTED_LIBRARY_QUICK_FIX = object : LocalQuickFix {
            override fun getFamilyName(): String = "Unsupported package"

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val element = descriptor.psiElement
                element.delete()
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
                            UnsupportedLibraryMessage(importPath),
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
                val importText = o.path
                if (unsupportedImports.contains(importText)) {
                    holder.registerProblem(
                        o,
                        UnsupportedLibraryMessage(importText),
                        ProblemHighlightType.GENERIC_ERROR,
                        UNSUPPORTED_LIBRARY_QUICK_FIX
                    )
                }
            }
        }
    }
}
