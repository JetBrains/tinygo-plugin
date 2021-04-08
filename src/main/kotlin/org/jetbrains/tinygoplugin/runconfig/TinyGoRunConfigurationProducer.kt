package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoRunConfigurationProducerBase
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class TinyGoRunConfigurationProducer : GoRunConfigurationProducerBase<TinyGoRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        TinyGoRunConfigurationType.getInstance().flashFactory

    override fun setupConfigurationFromContext(
        configuration: TinyGoRunConfiguration,
        context: ConfigurationContext,
        elementRef: Ref<PsiElement>,
    ): Boolean {
        val element = getContextElement(context)
        val contextFile = element?.containingFile
        val files = getFilesFromContext(context, contextFile)
        if (files.isEmpty()) {
            return false
        }
        val module = findModule(contextFile, context) ?: return false
        prepareConfigurationFromContext(configuration, module)
        configuration.name = "TinyGo ${module.name}"
        return true
    }

    override fun isConfigurationFromContext(
        configuration: TinyGoRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = getContextElement(context)
        val contextFile = element?.containingFile
        val module = findModule(contextFile, context) ?: return false
        return configuration.name == "TinyGo ${module.name}"
    }
}