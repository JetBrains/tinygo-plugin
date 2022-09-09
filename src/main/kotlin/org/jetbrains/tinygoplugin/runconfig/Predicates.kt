package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoConstants
import com.goide.execution.GoRunUtil
import com.goide.execution.testing.GoTestFinder
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.psi.PsiFile

fun isMainGoFile(file: PsiFile): Boolean = GoRunUtil.isMainGoFile(file)

fun isTestGoFile(file: PsiFile): Boolean = GoTestFinder.isTestFile(file)

fun isMainFunction(declaration: GoFunctionOrMethodDeclaration) = GoConstants.MAIN == declaration.name

fun isTestFunction(declaration: GoFunctionOrMethodDeclaration): Boolean =
    GoTestFinder.isTestOrExampleFunction(declaration)

fun isWasmTarget(targetPlatform: String): Boolean = targetPlatform in wasmTargets
private val wasmTargets = arrayOf("wasm", "wasi")
