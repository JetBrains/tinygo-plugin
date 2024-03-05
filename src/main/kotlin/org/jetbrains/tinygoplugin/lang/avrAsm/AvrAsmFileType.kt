package org.jetbrains.tinygoplugin.lang.avrAsm

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import javax.swing.Icon

object AvrAsmFileType : LanguageFileType(AvrAsmLanguage.INSTANCE) {
    override fun getName(): String = "AVR assembly"

    override fun getDescription(): String = "AVR assembly language"

    override fun getDefaultExtension(): String = "s"

    override fun getIcon(): Icon = TinyGoPluginIcons.TinyGoAsmIcon
}
