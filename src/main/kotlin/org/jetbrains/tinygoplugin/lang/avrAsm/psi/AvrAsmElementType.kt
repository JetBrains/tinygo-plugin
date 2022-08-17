package org.jetbrains.tinygoplugin.lang.avrAsm.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.tinygoplugin.lang.avrAsm.AvrAsmLanguage

class AvrAsmElementType(@NonNls debugName: String) : IElementType(debugName, AvrAsmLanguage.INSTANCE)
