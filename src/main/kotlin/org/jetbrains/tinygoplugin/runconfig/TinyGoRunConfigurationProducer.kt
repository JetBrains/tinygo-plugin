package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoRunConfigurationProducerBase
import com.goide.execution.GoRunUtil
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.nullSdk

class TinyGoRunConfigurationProducer : GoRunConfigurationProducerBase<TinyGoRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        TinyGoRunConfigurationType.getInstance().flashFactory

    @Suppress("ReturnCount")
    override fun setupConfigurationFromContext(
        configuration: TinyGoRunConfiguration,
        context: ConfigurationContext,
        elementRef: Ref<PsiElement>,
    ): Boolean {
        val settings = TinyGoConfiguration.getInstance(context.project)
        if (settings.sdk == nullSdk) return false
        val element = getContextElement(context)
        val contextFile = element?.containingFile

        val file = contextFile?.virtualFile ?: return false
        if (!GoRunUtil.isMainGoFile(contextFile)) return false
        val module = findModule(contextFile, context) ?: return false

        prepareConfigurationFromContext(configuration, module)
        configuration.name = "TinyGo ${module.name}"
        configuration.runConfig.mainFile = file.path
        return true
    }

    override fun isConfigurationFromContext(
        configuration: TinyGoRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = getContextElement(context)
        val contextFile = element?.containingFile
        val file = contextFile?.virtualFile ?: return false
        val module = findModule(contextFile, context) ?: return false
        return configuration.runConfig.mainFile == file.path &&
            configuration.name.startsWith("TinyGo ${module.name}")
    }
}
