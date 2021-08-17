package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoRunConfigurationProducerBase
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

abstract class TinyGoRunConfigurationProducer(private val configurationName: String) :
    GoRunConfigurationProducerBase<TinyGoRunConfiguration>() {

    protected open fun contextPredicate(contextFile: PsiFile): Boolean = isMainGoFile(contextFile)

    private fun getConfigurationFullName(unitName: String): String = "TinyGo $configurationName $unitName"

    protected open fun getConfigurationSubjectName(context: ConfigurationContext): String? {
        val element = getContextElement(context)
        val contextFile = element?.containingFile
        val module = findModule(contextFile, context)
        return module?.name
    }

    @Suppress("ReturnCount")
    final override fun setupConfigurationFromContext(
        configuration: TinyGoRunConfiguration,
        context: ConfigurationContext,
        elementRef: Ref<PsiElement>,
    ): Boolean {
        val settings = TinyGoConfiguration.getInstance(context.project)
        if (!settings.enabled) return false
        val element = getContextElement(context)
        val contextFile = element?.containingFile

        val file = contextFile?.virtualFile ?: return false
        if (!contextPredicate(contextFile)) return false
        val module = findModule(contextFile, context) ?: return false
        val configurationFullName = getConfigurationFullName(getConfigurationSubjectName(context)!!)

        prepareConfigurationFromContext(configuration, module)
        configuration.name = configurationFullName
        configuration.runConfig.mainFile = file.path
        return true
    }

    final override fun isConfigurationFromContext(
        configuration: TinyGoRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = getContextElement(context)
        val contextFile = element?.containingFile
        val file = contextFile?.virtualFile ?: return false
        val configurationFullName = getConfigurationFullName(getConfigurationSubjectName(context) ?: return false)
        return configuration.runConfig.mainFile == file.path && configuration.name.startsWith(configurationFullName)
    }
}

class TinyGoFlashRunConfigurationProducer : TinyGoRunConfigurationProducer("Flash") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        TinyGoRunConfigurationType.getInstance().flashFactory
}

class TinyGoEmulateRunConfigurationProducer : TinyGoRunConfigurationProducer("Emulate") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        TinyGoRunConfigurationType.getInstance().runFactory
}

class TinyGoTestRunConfigurationProducer : TinyGoRunConfigurationProducer("Test") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        TinyGoRunConfigurationType.getInstance().testFactory

    override fun contextPredicate(contextFile: PsiFile): Boolean = isTestGoFile(contextFile)

    override fun getConfigurationSubjectName(context: ConfigurationContext): String {
        val element = getContextElement(context)
        val contextFile = element?.containingFile
        val file = contextFile!!.virtualFile
        return file.name
    }
}
