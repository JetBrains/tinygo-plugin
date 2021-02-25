package org.jetbrains.tinygoplugin.inspections.libraries

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoInspectionMessage
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoImportSpec
import com.goide.psi.GoVisitor
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.services.SupportedLibrariesFilter
import org.jetbrains.tinygoplugin.services.TinyGoSupportedPackages

class UnsupportedLibraryMessage(private val packageName: String) : GoInspectionMessage {
    override fun getTemplate(): String = ""
    override fun toString(): String {
        val tinyGoLink = "https://tinygo.org/lang-support/stdlib/#${packageName.replace('/', '-')}"
        val tinyGoLinkHttp = """<a href="$tinyGoLink">$packageName</a>"""
        return "<html>$packageName is not supported by TinyGo. For details see $tinyGoLinkHttp</html>"
    }
}

class UnsupportedLibrariesException(
    private val filter: SupportedLibrariesFilter,
    private val holder: GoProblemsHolder,
) : GoVisitor() {
    override fun visitImportSpec(o: GoImportSpec) {
        super.visitImportSpec(o)
        if (o.isCImport || o.isBlank) {
            return
        }
        val importText = o.path
        if (!filter.check(importText)) {
            holder.registerProblem(
                o,
                UnsupportedLibraryMessage(importText),
                ProblemHighlightType.GENERIC_ERROR,
                ImportInspection.UNSUPPORTED_LIBRARY_QUICK_FIX
            )
        }
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
        problemsHolder: GoProblemsHolder,
        locationInspection: LocalInspectionToolSession,
    ): GoVisitor {
        val project = problemsHolder.manager.project
        val librariesFilter = project.service<TinyGoSupportedPackages>()
        return UnsupportedLibrariesException(librariesFilter.supportedPackages, problemsHolder)
    }
}
