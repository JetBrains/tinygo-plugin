package org.jetbrains.tinygoplugin.lang.avrAsm

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.jetbrains.tinygoplugin.lang.avrAsm.psi.AvrAsmTypes

class AvrAsmSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return AvrAsmLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return ourAttributes.getOrDefault(tokenType, TextAttributesKey.EMPTY_ARRAY)
    }

    companion object {
        private val PREPROCESSOR = TextAttributesKey.createTextAttributesKey(
            "AVR_PREPROCESSOR",
            DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE
        )
        private val LINE_COMMENT = TextAttributesKey.createTextAttributesKey(
            "AVR_LINE_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        private val BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey(
            "AVR_BLOCK_COMMENT",
            DefaultLanguageHighlighterColors.BLOCK_COMMENT
        )
        private val LABEL = TextAttributesKey.createTextAttributesKey(
            "AVR_LABEL",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )

        private val DIRECTIVE = TextAttributesKey.createTextAttributesKey(
            "AVR_DIRECTIVE_NAME",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        private val MNEMONIC = TextAttributesKey.createTextAttributesKey(
            "AVR_MNEMONIC",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        private val PC = TextAttributesKey.createTextAttributesKey(
            "AVR_PC",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        private val REGISTER = TextAttributesKey.createTextAttributesKey(
            "AVR_REGISTER",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        private val FUNC = TextAttributesKey.createTextAttributesKey(
            "AVR_CALL",
            DefaultLanguageHighlighterColors.STATIC_METHOD
        )
        private val NUMBER = TextAttributesKey.createTextAttributesKey(
            "AVR_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )
        private val STRING = TextAttributesKey.createTextAttributesKey(
            "AVR_STRING",
            DefaultLanguageHighlighterColors.STRING
        )

        private val CONST = TextAttributesKey.createTextAttributesKey(
            "AVR_CONST",
            DefaultLanguageHighlighterColors.CONSTANT
        )
        private val VARIABLE = TextAttributesKey.createTextAttributesKey(
            "AVR_VARIABLE",
            DefaultLanguageHighlighterColors.GLOBAL_VARIABLE
        )

        private val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
            "AVR_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )
        private val COMMA = TextAttributesKey.createTextAttributesKey(
            "AVR_COMMA",
            DefaultLanguageHighlighterColors.COMMA
        )
        private val SIGN = TextAttributesKey.createTextAttributesKey(
            "AVR_SIGN",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        private val PARENTHESES = TextAttributesKey.createTextAttributesKey(
            "AVR_PARENTHESES",
            DefaultLanguageHighlighterColors.PARENTHESES
        )

        private val ourAttributes: MutableMap<IElementType, Array<TextAttributesKey>> = HashMap()

        init {
            ourAttributes[AvrAsmTypes.PREPROCESSOR] = pack(PREPROCESSOR)
            ourAttributes[AvrAsmTypes.LINE_COMMENT] = pack(LINE_COMMENT)
            ourAttributes[AvrAsmTypes.BLOCK_COMMENT] = pack(BLOCK_COMMENT)
            ourAttributes[AvrAsmTypes.LABEL] = pack(LABEL)

            ourAttributes[AvrAsmTypes.GENERIC_DIRECTIVE] = pack(DIRECTIVE)
            ourAttributes[AvrAsmTypes.SET_DIRECTIVE] = pack(DIRECTIVE)
            ourAttributes[AvrAsmTypes.EQU_DIRECTIVE] = pack(DIRECTIVE)
            ourAttributes[AvrAsmTypes.DEF_DIRECTIVE] = pack(DIRECTIVE)
            ourAttributes[AvrAsmTypes.DB_DIRECTIVE] = pack(DIRECTIVE)
            ourAttributes[AvrAsmTypes.REPORT_DIRECTIVE] = pack(DIRECTIVE)
            ourAttributes[AvrAsmTypes.MNEMONIC] = pack(MNEMONIC)
            ourAttributes[AvrAsmTypes.BR_MNEMONIC] = pack(MNEMONIC)

            ourAttributes[AvrAsmTypes.PC] = pack(PC)
            ourAttributes[AvrAsmTypes.REGISTER] = pack(REGISTER)
            ourAttributes[AvrAsmTypes.FUNC] = pack(FUNC)
            ourAttributes[AvrAsmTypes.DEFINED_FUNC] = pack(FUNC)
            ourAttributes[AvrAsmTypes.STRLEN_FUNC] = pack(FUNC)
            ourAttributes[AvrAsmTypes.INTEGER] = pack(NUMBER)
            ourAttributes[AvrAsmTypes.CHAR] = pack(NUMBER)
            ourAttributes[AvrAsmTypes.STRING] = pack(STRING)

            ourAttributes[AvrAsmTypes.CONST] = pack(CONST)
            ourAttributes[AvrAsmTypes.VARIABLE] = pack(VARIABLE)

            ourAttributes[TokenType.BAD_CHARACTER] = pack(BAD_CHARACTER)
            ourAttributes[AvrAsmTypes.MINUS] = pack(SIGN)
            ourAttributes[AvrAsmTypes.COMMA] = pack(COMMA)
            ourAttributes[AvrAsmTypes.L_PAREN] = pack(PARENTHESES)
            ourAttributes[AvrAsmTypes.R_PAREN] = pack(PARENTHESES)
        }
    }
}
