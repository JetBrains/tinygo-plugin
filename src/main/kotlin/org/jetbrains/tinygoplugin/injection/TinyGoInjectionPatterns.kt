package org.jetbrains.tinygoplugin.injection

import com.goide.completion.GoCompletionUtil
import com.goide.psi.GoArgumentList
import com.goide.psi.GoCallExpr
import com.goide.psi.GoReferenceExpression
import com.goide.psi.GoStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.util.ProcessingContext

object TinyGoInjectionPatterns : PlatformPatterns() {
    @JvmStatic
    fun tinyGoInlineAssembly(device: String): PsiElementPattern.Capture<GoStringLiteral> =
        psiElement(GoStringLiteral::class.java)
            .withAncestor(1, psiElement(GoArgumentList::class.java))
            .withAncestor(2, asmInjectionFunctionCall(device))

    @JvmStatic
    private fun asmInjectionFunctionCall(device: String) =
        psiElement(GoCallExpr::class.java)
            .withChild(asmInjectionFunctionReference(device))

    @JvmStatic
    private fun asmInjectionFunctionReference(device: String) =
        psiElement(GoReferenceExpression::class.java)
            .with(
                GoCompletionUtil.condition("string value is assembly injection function name") {
                    val identifier = it.identifier.text
                    (identifier == "Asm") || (identifier == "AsmFull")
                }
            ).withChild(
                supportedDeviceLibraryReference(device)
            )

    @JvmStatic
    private fun supportedDeviceLibraryReference(device: String) =
        object : PsiElementPattern.Capture<GoReferenceExpression>(GoReferenceExpression::class.java) {
            override fun accepts(o: Any?, context: ProcessingContext): Boolean {
                if (o == null) return false
                val packageName = (o as GoReferenceExpression).text
                return packageName == device
            }
        }
}
