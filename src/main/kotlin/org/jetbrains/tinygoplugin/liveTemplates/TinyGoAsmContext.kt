package org.jetbrains.tinygoplugin.liveTemplates

import com.goide.psi.GoFile
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

abstract class TinyGoAsmContext(id: String, presentableName: String) : TemplateContextType(id, presentableName) {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val file = templateActionContext.file
        val project = file.project
        return file is GoFile && TinyGoConfiguration.getInstance(project).enabled
    }
}

class TinyGoAsmAvr : TinyGoAsmContext("TINYGOAVR", "TinyGo AVR") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val project = templateActionContext.file.project
        val goTags = TinyGoConfiguration.getInstance(project).goTags
        return super.isInContext(templateActionContext) && goTags.contains("avr")
    }
}

class TinyGoAsmArm : TinyGoAsmContext("TINYGOARM", "TinyGo ARM") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val project = templateActionContext.file.project
        val goTags = TinyGoConfiguration.getInstance(project).goTags.split(" ")

        val notArmDevices = setOf("avr", "arm64", "kendryte")

        return super.isInContext(templateActionContext) &&
            goTags.contains("arm") && !goTags.contains("avr") && !goTags.any { tag -> notArmDevices.contains(tag) }
    }
}

class TinyGoAsmArm64 : TinyGoAsmContext("TINYGOARM64", "TinyGo ARM64") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val project = templateActionContext.file.project
        val goTags = TinyGoConfiguration.getInstance(project).goTags
        return super.isInContext(templateActionContext) && goTags.contains("arm64")
    }
}
