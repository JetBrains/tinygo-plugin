package org.jetbrains.tinygoplugin.lang.avrAsm

import com.intellij.openapi.fileTypes.LanguageFileType
import icons.GOPlan9Icons
import javax.swing.Icon

class AvrAsmFileType : LanguageFileType(AvrAsmLanguage.INSTANCE) {
    companion object {
        val INSTANCE = AvrAsmFileType()
    }

    override fun getName(): String = "AVR assembly"

    override fun getDescription(): String = "AVR assembly language"

    override fun getDefaultExtension(): String = "s"

    override fun getIcon(): Icon = GOPlan9Icons.CPU
}
