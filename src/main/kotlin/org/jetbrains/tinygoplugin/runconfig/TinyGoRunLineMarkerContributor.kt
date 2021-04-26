package org.jetbrains.tinygoplugin.runconfig

import com.goide.GoConstants
import com.goide.GoTypes
import com.goide.execution.GoRunLineMarkerProvider
import com.goide.execution.GoRunUtil
import com.goide.psi.GoFunctionDeclaration
import com.goide.util.GoUtil
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import org.jetbrains.tinygoplugin.sdk.nullSdk

private val TOOLTIP_PROVIDER =
    Function { _: PsiElement? -> "Flash TinyGo" }

class TinyGoRunLineMarkerContributor : GoRunLineMarkerProvider() {
    @Suppress("MagicNumber", "ReturnCount")
    override fun getInfo(e: PsiElement): Info? {
        if (e.node.elementType === GoTypes.IDENTIFIER) {
            val parent = e.parent
            val file = e.containingFile
            if (InjectedLanguageManager.getInstance(e.project).isInjectedFragment(file)) {
                return null
            }
            val settings = TinyGoConfiguration.getInstance(e.project)
            if (settings.sdk == nullSdk) return null
            if (GoUtil.isInProject(file) && GoRunUtil.isMainGoFile(file) && parent is GoFunctionDeclaration) {
                if (GoConstants.MAIN == parent.name) {
                    val actions = ExecutorAction.getActions(1)
                    return Info(TinyGoPluginIcons.TinyGoIcon,
                        TOOLTIP_PROVIDER,
                        actions[0], actions[actions.size - 1])
                }
            }
        }
        return null
    }
}
