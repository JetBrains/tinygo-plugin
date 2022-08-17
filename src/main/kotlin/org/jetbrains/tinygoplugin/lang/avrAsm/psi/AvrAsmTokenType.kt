package org.jetbrains.tinygoplugin.lang.avrAsm.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.tinygoplugin.lang.avrAsm.AvrAsmLanguage

class AvrAsmTokenType(@NonNls debugName: String) : IElementType(debugName, AvrAsmLanguage.INSTANCE) {
    override fun toString(): String {
        return "AvrAsmTokenType." + super.toString()
    }
}
