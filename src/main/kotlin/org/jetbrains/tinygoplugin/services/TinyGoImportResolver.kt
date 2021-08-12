package org.jetbrains.tinygoplugin.services

import com.goide.psi.impl.GoPackage
import com.goide.psi.impl.GoPsiImplUtil
import com.goide.psi.impl.imports.GoImportReference
import com.goide.psi.impl.imports.GoImportResolver
import com.goide.util.GoUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

class TinyGoImportResolver : GoImportResolver {
    override fun resolve(
        importPath: String,
        project: Project,
        module: Module?,
        resolveState: ResolveState?,
    ): Collection<GoPackage>? {
        if (!TinyGoConfiguration.getInstance(project).enabled) {
            return null
        }
        return innerResolve(importPath, project, module)
    }

    override fun resolve(reference: GoImportReference): Array<ResolveResult>? {
        val element = reference.element
        val project = element.project
        if (!TinyGoConfiguration.getInstance(project).enabled) {
            return null
        }
        val module = GoUtil.module(element)
        // extract import
        val importPath = extractImportPath(reference)
        val resolveResult = innerResolve(importPath, project, module)
        return resolveResult?.asSequence()?.map { it.directories }?.flatten()?.filterNotNull()
            ?.mapNotNull { PsiManager.getInstance(project).findDirectory(it) }
            ?.map { PsiElementResolveResult(it) }?.toList()?.toTypedArray()
    }

    private fun innerResolve(
        importPath: String,
        project: Project,
        module: Module?,
    ): Collection<GoPackage>? {
        if (importPath.isEmpty()) {
            return emptyList()
        }
        val tinyGoCachedGoRoot = TinyGoConfiguration.getInstance(project).cachedGoRoot
        val tinyGoCachedGoRootSrc = tinyGoCachedGoRoot.srcDir
        val importFile = tinyGoCachedGoRootSrc?.findFileByRelativePath(importPath)
        return if (importFile != null) {
            GoPackage.`in`(PsiManager.getInstance(project).findDirectory(importFile), module)
        } else {
            null
        }
    }
}

// Plan to use for caching
internal fun extractFile(resolveState: ResolveState?, module: Module?, project: Project): UserDataHolder {
    val context = GoPsiImplUtil.getContextElement(resolveState)
    val file = context?.containingFile
    // return the first thing that is not null among original file, module or project
    return file?.originalFile ?: module ?: project
}

internal fun extractImportPath(reference: GoImportReference): String {
    val allReferences = reference.fileReferenceSet.allReferences
    val applicableReferences = allReferences.filter { it.index <= reference.index }
    val imports = applicableReferences.map { if (it.index > 0) "/" + it.canonicalText else it.canonicalText }
    return imports.joinToString()
}
