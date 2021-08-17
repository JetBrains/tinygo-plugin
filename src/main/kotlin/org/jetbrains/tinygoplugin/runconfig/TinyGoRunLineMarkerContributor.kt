package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoTypes
import com.goide.execution.GoRunLineMarkerProvider
import com.goide.execution.testing.GoTestRunLineMarkerProvider
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.util.GoUtil
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor.Info
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons

interface TinyGoFunctionRunLineMarkerContributor {
    @Suppress("MagicNumber", "ReturnCount")
    fun getRunLineMarkerInfoForFunction(e: PsiElement): Info? {
        if (e.node.elementType === GoTypes.IDENTIFIER) {
            val parent = e.parent
            val file = e.containingFile
            if (InjectedLanguageManager.getInstance(e.project).isInjectedFragment(file)) {
                return null
            }
            val settings = TinyGoConfiguration.getInstance(e.project)
            if (!settings.enabled) return null
            if (GoUtil.isInProject(file) && filePredicate(file) && parent is GoFunctionDeclaration) {
                if (functionPredicate(parent)) {
                    val actions = ExecutorAction.getActions(1)
                    return Info(
                        TinyGoPluginIcons.TinyGoIcon,
                        this::tooltipProvider,
                        actions[0], actions[actions.size - 1]
                    )
                }
            }
        }
        return null
    }

    fun tooltipProvider(e: PsiElement): String

    fun filePredicate(file: PsiFile): Boolean

    fun functionPredicate(declaration: GoFunctionOrMethodDeclaration): Boolean
}

class TinyGoRunLineMarkerContributor : TinyGoFunctionRunLineMarkerContributor, GoRunLineMarkerProvider() {
    override fun getInfo(e: PsiElement): Info? = getRunLineMarkerInfoForFunction(e)

    override fun tooltipProvider(e: PsiElement): String = "Flash TinyGo"

    override fun filePredicate(file: PsiFile): Boolean = isMainGoFile(file)

    override fun functionPredicate(declaration: GoFunctionOrMethodDeclaration): Boolean = isMainFunction(declaration)
}

class TinyGoTestRunLineMarkerContributor : TinyGoFunctionRunLineMarkerContributor, GoTestRunLineMarkerProvider() {
    override fun getInfo(e: PsiElement): Info? = getRunLineMarkerInfoForFunction(e)

    override fun tooltipProvider(e: PsiElement): String = "Test TinyGo"

    override fun filePredicate(file: PsiFile): Boolean = isTestGoFile(file)

    override fun functionPredicate(declaration: GoFunctionOrMethodDeclaration): Boolean = isTestFunction(declaration)
}
