package org.jetbrains.tinygoplugin.inspections.libraries

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoImportSpec
import com.goide.psi.GoVisitor
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.yaml.snakeyaml.Yaml

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
                { "$importText is not supported by tinyGo" },
                ProblemHighlightType.GENERIC_ERROR,
                ImportInspection.UNSUPPORTED_LIBRARY_QUICK_FIX
            )
        }
    }
}

interface SupportedLibrariesFilter {
    fun check(name: String): Boolean
}

class DummyLibraryFilter : SupportedLibrariesFilter {
    override fun check(name: String): Boolean = true
}

class LibraryFilter(private val supportedMap: Map<String, Boolean>) : SupportedLibrariesFilter {
    private val defaultPolicy = true
    override fun check(name: String): Boolean = supportedMap.getOrDefault(name, defaultPolicy)
}

class ImportInspection : GoInspectionBase() {
    private val filter: Lazy<SupportedLibrariesFilter> = lazy {
        val stream = this.javaClass.classLoader.getResourceAsStream("libraries/latest.yaml")
        if (stream == null) {
            thisLogger().warn("Could not load list of supported libraries")
            DummyLibraryFilter()
        } else {
            val data: Map<String, Any> = Yaml().load(stream)
            val supportedLibraries = data.mapValues { entry ->
                if (entry.value is Boolean) entry.value as Boolean else true
            }
            LibraryFilter(supportedLibraries)
        }
    }

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
        return UnsupportedLibrariesException(filter.value, problemsHolder)
    }
}
