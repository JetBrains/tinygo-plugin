package org.jetbrains.tinygoplugin.lang.avrAsm

import com.intellij.lexer.FlexAdapter

class AvrAsmLexerAdapter : FlexAdapter(AvrAsmLexer(null))
