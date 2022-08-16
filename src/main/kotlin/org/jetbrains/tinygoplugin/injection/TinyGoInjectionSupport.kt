package org.jetbrains.tinygoplugin.injection

import com.goide.psi.GoStringLiteral
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiLanguageInjectionHost
import org.intellij.plugins.intelliLang.inject.AbstractLanguageInjectionSupport
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

class TinyGoInjectionSupport : AbstractLanguageInjectionSupport() {
    override fun getId(): String = ID

    override fun isApplicableTo(host: PsiLanguageInjectionHost?): Boolean {
        if (host !is GoStringLiteral) return false
        val tinyGoConfiguration = TinyGoConfiguration.getInstance(host.project)
        return !ApplicationManager.getApplication().isHeadlessEnvironment &&
            tinyGoConfiguration.enabled && (
            tinyGoConfiguration.goTags.contains("avr") ||
                tinyGoConfiguration.goTags.contains("arm")
            )
    }

    override fun useDefaultInjector(host: PsiLanguageInjectionHost?): Boolean = true

    override fun getPatternClasses(): Array<Class<*>> = arrayOf(TinyGoInjectionPatterns::class.java)

    // Seems like the only way to disable default comment injector.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun useDefaultCommentInjector(): Boolean = false

    companion object {
        private const val ID = "tinygo"
    }
}
