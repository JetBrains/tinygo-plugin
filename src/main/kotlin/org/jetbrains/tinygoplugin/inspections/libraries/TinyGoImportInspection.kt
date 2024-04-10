package org.jetbrains.tinygoplugin.inspections.libraries

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoFile
import com.goide.psi.GoImportSpec
import com.goide.psi.GoNamedElement
import com.goide.psi.GoPointerType
import com.goide.psi.GoReferenceExpression
import com.goide.psi.GoType
import com.goide.psi.GoVarOrConstDefinition
import com.goide.psi.GoVisitor
import com.goide.psi.impl.GoPackage
import com.goide.util.GoUtil
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.inspections.inspectionMessage
import org.jetbrains.tinygoplugin.runconfig.isTestGoFile
import org.jetbrains.tinygoplugin.services.UnsupportedPackageProvider

private fun tinyGoLink(packageName: String): String =
    "https://tinygo.org/lang-support/stdlib/#${packageName.replace("/", "")}"

open class TinyGoImportInspection : GoInspectionBase() {
    protected open val produceLink: Boolean = true
    protected open val defaultInspectionMessage = IMPORT_INSPECTION_MESSAGE

    protected fun GoProblemsHolder.registerImportProblem(
        o: PsiElement,
        messageName: String,
        importPath: String,
        packageName: String
    ) {
        val concatenatedMessage = if (importPath.isEmpty()) inspectionMessage(messageName)
        else if (produceLink) inspectionMessage(messageName, importPath, tinyGoLink(importPath))
        else inspectionMessage(messageName, importPath, packageName)

        registerProblem(
            o,
            concatenatedMessage,
            ProblemHighlightType.GENERIC_ERROR,
            UNSUPPORTED_LIBRARY_QUICK_FIX
        )
    }

    protected open fun getDefinedUnsupportedPackages(project: Project): Set<String> =
        project.service<UnsupportedPackageProvider>().unsupportedLibraries()

    protected open fun customContextPredicate(o: PsiElement): Boolean = true

    override fun buildVisitor(
        holder: GoProblemsHolder,
        locationInspection: LocalInspectionToolSession,
    ): GoVisitor {
        val project = holder.manager.project
        val definedUnsupportedImports = getDefinedUnsupportedPackages(project)
        val computedUnsupportedImports = mutableSetOf<String>()

        return object : GoVisitor() {
            override fun visitReferenceExpression(reference: GoReferenceExpression) {
                super.visitReferenceExpression(reference)
                if (!reference.project.tinyGoConfiguration().enabled || !customContextPredicate(reference)) {
                    return
                }
                val target = reference.resolve()
                if (target is GoVarOrConstDefinition) {
                    val type = unwrapPointerTypeIfNeeded(target.getGoType(ResolveState.initial())) ?: return
                    checkType(type, reference)
                }
                val file = target?.containingFile
                if (file is GoFile) {
                    checkFileLocation(
                        file,
                        reference,
                        IMPORT_DEPENDENCY_INSPECTION_MESSAGE,
                        generateDisplayName(target)
                    )
                }
            }

            override fun visitImportSpec(import: GoImportSpec) {
                super.visitImportSpec(import)
                val importConditions = import.isCImport || import.isBlank
                val tinyGoContextConditions =
                    !import.project.tinyGoConfiguration().enabled || !customContextPredicate(import)
                if (importConditions || tinyGoContextConditions) {
                    return
                }
                if (!checkFileLocation(
                        import.path,
                        import,
                        defaultInspectionMessage,
                        IMPORT_DEPENDENCY_INSPECTION_MESSAGE,
                        generateDisplayName(import)
                    )
                ) {
                    computedUnsupportedImports.add(import.path)
                    return
                }

                val packages = import.resolve(ResolveState.initial())
                val importedPkgRefersToBadLib = packages.stream().anyMatch { pkg ->
                    pkg.files().stream().anyMatch {
                        if (it is GoFile) {
                            if (!GoUtil.matchedForModuleBuildTarget(it, GoUtil.module(it))) false
                            else !checkFileImports(
                                it,
                                import,
                                generateDisplayName(import)
                            )
                        } else true
                    }
                }
                if (importedPkgRefersToBadLib) computedUnsupportedImports.add(import.path)
            }

            override fun visitType(type: GoType) {
                super.visitType(type)
                if (!project.tinyGoConfiguration().enabled || !customContextPredicate(type)) {
                    return
                }
                val resolvedType = unwrapPointerTypeIfNeeded(type) ?: return
                checkType(resolvedType, type)
            }

            private fun unwrapPointerTypeIfNeeded(type: GoType?): GoType? =
                if (type is GoPointerType) type.type else type

            private fun checkFileLocation(
                importPath: String,
                element: PsiElement,
                messageNameDef: String,
                messageNameComp: String,
                arg: String
            ): Boolean {
                val problem = if (definedUnsupportedImports.contains(importPath)) messageNameDef
                else if (computedUnsupportedImports.contains(importPath)) messageNameComp
                else ""
                return if (problem.isNotEmpty()) {
                    holder.registerImportProblem(element, problem, arg, importPath)
                    false
                } else true
            }

            @RequiresReadLock
            @RequiresBackgroundThread
            private fun checkFileLocation(
                file: GoFile,
                element: PsiElement,
                messageName: String,
                arg: String
            ): Boolean {
                val importPath = getImportPath(file) ?: return true
                return checkFileLocation(importPath, element, messageName, messageName, arg)
            }

            @Suppress("ReturnCount")
            private fun checkFileImports(file: GoFile, element: PsiElement, arg: String): Boolean {
                if (isTestGoFile(file)) return true // ignore library tests they won't compile with the project
                val fileImports = file.imports
                for (fileImport in fileImports) {
                    if (
                        definedUnsupportedImports.contains(fileImport.path) ||
                        computedUnsupportedImports.contains(fileImport.path)
                    ) {
                        holder.registerImportProblem(
                            element,
                            IMPORT_DEPENDENCY_INSPECTION_MESSAGE,
                            arg,
                            fileImport.path
                        )
                        return false
                    }
                }
                return true
            }

            private fun checkType(type: GoType, elementToBlame: PsiElement) {
                val file = type.contextlessResolve()?.containingFile ?: return
                if (file is GoFile) {
                    checkFileLocation(
                        file,
                        elementToBlame,
                        IMPORT_DEPENDENCY_TYPE_INSPECTION_MESSAGE,
                        generateDisplayName(type)
                    )
                }
            }

            @RequiresReadLock
            @RequiresBackgroundThread
            private fun getImportPath(file: GoFile): String? = file.getImportPath(false)
                ?: tryToFindImportPathAlt(file)

            @RequiresReadLock
            @RequiresBackgroundThread
            private fun tryToFindImportPathAlt(file: GoFile): String? {
                val pkg = GoPackage.of(file)
                val dir = pkg?.directories?.first() ?: return null
                val goRoot = holder.manager.project.tinyGoConfiguration().cachedGoRoot.sdkRoot ?: return null
                return if (VfsUtil.isAncestor(goRoot, dir, false)) {
                    dir.toString().removePrefix("$goRoot/src/")
                } else null
            }

            private fun generateDisplayName(element: PsiElement) = when (element) {
                is GoType ->
                    element.presentationText
                is GoImportSpec ->
                    element.path
                is GoNamedElement ->
                    element.presentation?.presentableText ?: element.text
                else -> element.text
            }
        }
    }
}

private const val IMPORT_INSPECTION_MESSAGE = "inspection.import.unsupported.message"
private const val IMPORT_QUICK_FIX_FAMILY = "inspection.import.unsupported.fix"
private const val IMPORT_DEPENDENCY_INSPECTION_MESSAGE = "inspection.import.reference.message"
private const val IMPORT_DEPENDENCY_TYPE_INSPECTION_MESSAGE = "inspection.import.type.message"

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
