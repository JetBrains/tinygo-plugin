package org.jetbrains.tinygoplugin.lang.avrAsm

import com.intellij.lang.Language

class AvrAsmLanguage : Language("AVRASM") {
    companion object {
        val INSTANCE = AvrAsmLanguage()
    }
}
