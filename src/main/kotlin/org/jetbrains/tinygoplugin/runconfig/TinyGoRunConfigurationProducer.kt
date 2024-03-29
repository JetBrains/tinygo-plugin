package org.jetbrains.tinygoplugin.runconfig

import com.goide.execution.GoBuildingRunConfiguration
import com.goide.execution.GoRunConfigurationProducerBase
import com.goide.execution.application.GoApplicationConfiguration
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration

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
        val settings = context.project.tinyGoConfiguration()
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

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        return when (other.configuration) {
            is GoApplicationConfiguration -> true
            is GoBuildingRunConfiguration<*> -> true
            else -> false
        }
    }
}

abstract class TinyGoHardwareRunConfigurationProducer(configurationName: String) :
    TinyGoRunConfigurationProducer(configurationName) {
    override fun contextPredicate(contextFile: PsiFile): Boolean {
        val targetsWasm = isWasmTarget(contextFile.project.tinyGoConfiguration().targetPlatform)
        return super.contextPredicate(contextFile) && !targetsWasm
    }
}

class TinyGoFlashRunConfigurationProducer : TinyGoHardwareRunConfigurationProducer("Flash") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        runConfigurationType<TinyGoRunConfigurationType>().flashFactory
}

class TinyGoEmulateRunConfigurationProducer : TinyGoRunConfigurationProducer("Emulate") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        runConfigurationType<TinyGoRunConfigurationType>().runFactory
}

class TinyGoTestRunConfigurationProducer : TinyGoRunConfigurationProducer("Test") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        runConfigurationType<TinyGoRunConfigurationType>().testFactory

    override fun contextPredicate(contextFile: PsiFile): Boolean = isTestGoFile(contextFile)

    override fun getConfigurationSubjectName(context: ConfigurationContext): String {
        val element = getContextElement(context)
        val contextFile = element?.containingFile
        val file = contextFile!!.virtualFile
        return file.name
    }
}

class TinyGoBuildRunConfigurationProducer : TinyGoRunConfigurationProducer("Build") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        runConfigurationType<TinyGoRunConfigurationType>().buildFactory
}

class TinyGoHeapAllocRunConfigurationProducer : TinyGoRunConfigurationProducer("Heap Allocations") {
    override fun getConfigurationFactory(): ConfigurationFactory =
        runConfigurationType<TinyGoRunConfigurationType>().heapAllocFactory
}
