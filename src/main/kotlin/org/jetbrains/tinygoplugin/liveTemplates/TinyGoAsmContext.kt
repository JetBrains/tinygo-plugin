package org.jetbrains.tinygoplugin.liveTemplates

import com.goide.psi.GoFile
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration

abstract class TinyGoAsmContext(presentableName: String) : TemplateContextType(presentableName) {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val file = templateActionContext.file
        val project = file.project
        return file is GoFile && project.tinyGoConfiguration().enabled
    }
}

class TinyGoAsmAvr : TinyGoAsmContext("TinyGo AVR") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val project = templateActionContext.file.project
        val goTags = project.tinyGoConfiguration().goTags.split(" ")
        return super.isInContext(templateActionContext) && goTags.contains("avr")
    }
}

class TinyGoAsmArm : TinyGoAsmContext("TinyGo ARM") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val project = templateActionContext.file.project
        val goTags = project.tinyGoConfiguration().goTags.split(" ")

        val notArm32BitDevices = setOf("avr", "arm64", "kendryte")

        return super.isInContext(templateActionContext) &&
            goTags.contains("arm") && goTags.none { tag -> notArm32BitDevices.contains(tag) }
    }
}

class TinyGoAsmArm64 : TinyGoAsmContext("TinyGo ARM64") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        val project = templateActionContext.file.project
        val goTags = project.tinyGoConfiguration().goTags.split(" ")
        return super.isInContext(templateActionContext) && goTags.contains("arm64")
    }
}
