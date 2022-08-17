// This is a generated file. Not intended for manual editing.
package org.jetbrains.tinygoplugin.lang.avrAsm.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.tinygoplugin.lang.avrAsm.psi.impl.*;

public interface AvrAsmTypes {

  IElementType ARGUMENT = new AvrAsmElementType("ARGUMENT");
  IElementType CALL = new AvrAsmElementType("CALL");
  IElementType CONST = new AvrAsmElementType("CONST");
  IElementType DIRECTIVE = new AvrAsmElementType("DIRECTIVE");
  IElementType EXPRESSION = new AvrAsmElementType("EXPRESSION");
  IElementType INDIRECT_POSTFIX = new AvrAsmElementType("INDIRECT_POSTFIX");
  IElementType INDIRECT_PREFIX = new AvrAsmElementType("INDIRECT_PREFIX");
  IElementType INSTRUCTION = new AvrAsmElementType("INSTRUCTION");
  IElementType MEMORY = new AvrAsmElementType("MEMORY");
  IElementType NUMBER = new AvrAsmElementType("NUMBER");
  IElementType OPERAND = new AvrAsmElementType("OPERAND");
  IElementType PREPROCESSOR = new AvrAsmElementType("PREPROCESSOR");
  IElementType SYMBOL = new AvrAsmElementType("SYMBOL");
  IElementType VARIABLE = new AvrAsmElementType("VARIABLE");

  IElementType BLOCK_COMMENT = new AvrAsmTokenType("BLOCK_COMMENT");
  IElementType BR_MNEMONIC = new AvrAsmTokenType("BR_MNEMONIC");
  IElementType CHAR = new AvrAsmTokenType("CHAR");
  IElementType COMMA = new AvrAsmTokenType("COMMA");
  IElementType DB_DIRECTIVE = new AvrAsmTokenType("DB_DIRECTIVE");
  IElementType DEFINED_FUNC = new AvrAsmTokenType("DEFINED_FUNC");
  IElementType DEF_DIRECTIVE = new AvrAsmTokenType("DEF_DIRECTIVE");
  IElementType EQUAL = new AvrAsmTokenType("EQUAL");
  IElementType EQU_DIRECTIVE = new AvrAsmTokenType("EQU_DIRECTIVE");
  IElementType FUNC = new AvrAsmTokenType("FUNC");
  IElementType GENERIC_DIRECTIVE = new AvrAsmTokenType("GENERIC_DIRECTIVE");
  IElementType IDENTIFIER = new AvrAsmTokenType("IDENTIFIER");
  IElementType INDIRECT = new AvrAsmTokenType("INDIRECT");
  IElementType INTEGER = new AvrAsmTokenType("INTEGER");
  IElementType LABEL = new AvrAsmTokenType("LABEL");
  IElementType LINE_COMMENT = new AvrAsmTokenType("LINE_COMMENT");
  IElementType L_PAREN = new AvrAsmTokenType("L_PAREN");
  IElementType MACROS_NAME = new AvrAsmTokenType("MACROS_NAME");
  IElementType MINUS = new AvrAsmTokenType("MINUS");
  IElementType MNEMONIC = new AvrAsmTokenType("MNEMONIC");
  IElementType PC = new AvrAsmTokenType("PC");
  IElementType PLUS = new AvrAsmTokenType("PLUS");
  IElementType PREPROCESSOR_NAME = new AvrAsmTokenType("PREPROCESSOR_NAME");
  IElementType REGISTER = new AvrAsmTokenType("REGISTER");
  IElementType REPORT_DIRECTIVE = new AvrAsmTokenType("REPORT_DIRECTIVE");
  IElementType R_PAREN = new AvrAsmTokenType("R_PAREN");
  IElementType SET_DIRECTIVE = new AvrAsmTokenType("SET_DIRECTIVE");
  IElementType STRING = new AvrAsmTokenType("STRING");
  IElementType STRLEN_FUNC = new AvrAsmTokenType("STRLEN_FUNC");
  IElementType SYMBOL_IDENTIFIER = new AvrAsmTokenType("SYMBOL_IDENTIFIER");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ARGUMENT) {
        return new AvrAsmArgumentImpl(node);
      }
      else if (type == CALL) {
        return new AvrAsmCallImpl(node);
      }
      else if (type == CONST) {
        return new AvrAsmConstImpl(node);
      }
      else if (type == DIRECTIVE) {
        return new AvrAsmDirectiveImpl(node);
      }
      else if (type == EXPRESSION) {
        return new AvrAsmExpressionImpl(node);
      }
      else if (type == INDIRECT_POSTFIX) {
        return new AvrAsmIndirectPostfixImpl(node);
      }
      else if (type == INDIRECT_PREFIX) {
        return new AvrAsmIndirectPrefixImpl(node);
      }
      else if (type == INSTRUCTION) {
        return new AvrAsmInstructionImpl(node);
      }
      else if (type == MEMORY) {
        return new AvrAsmMemoryImpl(node);
      }
      else if (type == NUMBER) {
        return new AvrAsmNumberImpl(node);
      }
      else if (type == OPERAND) {
        return new AvrAsmOperandImpl(node);
      }
      else if (type == PREPROCESSOR) {
        return new AvrAsmPreprocessorImpl(node);
      }
      else if (type == SYMBOL) {
        return new AvrAsmSymbolImpl(node);
      }
      else if (type == VARIABLE) {
        return new AvrAsmVariableImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
