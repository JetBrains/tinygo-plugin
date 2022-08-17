// This is a generated file. Not intended for manual editing.
package org.jetbrains.tinygoplugin.lang.avrAsm.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.jetbrains.tinygoplugin.lang.avrAsm.psi.AvrAsmTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class AvrAsmParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return asmFile(b, l + 1);
  }

  /* ********************************************************** */
  // memory | expression
  public static boolean argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT, "<argument>");
    r = memory(b, l + 1);
    if (!r) r = expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (preprocessor | LABEL | instruction | directive | LINE_COMMENT | BLOCK_COMMENT)*
  static boolean asmFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "asmFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!asmFile_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "asmFile", c)) break;
    }
    return true;
  }

  // preprocessor | LABEL | instruction | directive | LINE_COMMENT | BLOCK_COMMENT
  private static boolean asmFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "asmFile_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = preprocessor(b, l + 1);
    if (!r) r = consumeToken(b, LABEL);
    if (!r) r = instruction(b, l + 1);
    if (!r) r = directive(b, l + 1);
    if (!r) r = consumeToken(b, LINE_COMMENT);
    if (!r) r = consumeToken(b, BLOCK_COMMENT);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // BR_MNEMONIC (IDENTIFIER | (INTEGER COMMA IDENTIFIER))?
  static boolean branch_instruction(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "branch_instruction")) return false;
    if (!nextTokenIs(b, BR_MNEMONIC)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BR_MNEMONIC);
    r = r && branch_instruction_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (IDENTIFIER | (INTEGER COMMA IDENTIFIER))?
  private static boolean branch_instruction_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "branch_instruction_1")) return false;
    branch_instruction_1_0(b, l + 1);
    return true;
  }

  // IDENTIFIER | (INTEGER COMMA IDENTIFIER)
  private static boolean branch_instruction_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "branch_instruction_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = branch_instruction_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // INTEGER COMMA IDENTIFIER
  private static boolean branch_instruction_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "branch_instruction_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, INTEGER, COMMA, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // defined | strlen | generic_funk
  public static boolean call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "call")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CALL, "<call>");
    r = defined(b, l + 1);
    if (!r) r = strlen(b, l + 1);
    if (!r) r = generic_funk(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // symbol
  public static boolean const_$(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "const_$")) return false;
    if (!nextTokenIs(b, "<const $>", IDENTIFIER, SYMBOL_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONST, "<const $>");
    r = symbol(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // DB_DIRECTIVE <<list_of (expression | STRING)>>
  static boolean db_directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "db_directive")) return false;
    if (!nextTokenIs(b, DB_DIRECTIVE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DB_DIRECTIVE);
    r = r && list_of(b, l + 1, AvrAsmParser::db_directive_1_0);
    exit_section_(b, m, null, r);
    return r;
  }

  // expression | STRING
  private static boolean db_directive_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "db_directive_1_0")) return false;
    boolean r;
    r = expression(b, l + 1);
    if (!r) r = consumeToken(b, STRING);
    return r;
  }

  /* ********************************************************** */
  // DEF_DIRECTIVE symbol EQUAL REGISTER
  static boolean def_directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def_directive")) return false;
    if (!nextTokenIs(b, DEF_DIRECTIVE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DEF_DIRECTIVE);
    r = r && symbol(b, l + 1);
    r = r && consumeTokens(b, 0, EQUAL, REGISTER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // DEFINED_FUNC <<parenthesized (symbol)>>
  static boolean defined(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "defined")) return false;
    if (!nextTokenIs(b, DEFINED_FUNC)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DEFINED_FUNC);
    r = r && parenthesized(b, l + 1, AvrAsmParser::defined_1_0);
    exit_section_(b, m, null, r);
    return r;
  }

  // (symbol)
  private static boolean defined_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "defined_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = symbol(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // set_directive | equ_directive | def_directive | db_directive | report_directive | generic_directive
  public static boolean directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "directive")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, DIRECTIVE, "<directive>");
    r = set_directive(b, l + 1);
    if (!r) r = equ_directive(b, l + 1);
    if (!r) r = def_directive(b, l + 1);
    if (!r) r = db_directive(b, l + 1);
    if (!r) r = report_directive(b, l + 1);
    if (!r) r = generic_directive(b, l + 1);
    exit_section_(b, l, m, r, false, AvrAsmParser::recovery);
    return r;
  }

  /* ********************************************************** */
  // GENERIC_DIRECTIVE | EQU_DIRECTIVE | SET_DIRECTIVE | DEF_DIRECTIVE | DB_DIRECTIVE | REPORT_DIRECTIVE
  static boolean directives(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "directives")) return false;
    boolean r;
    r = consumeToken(b, GENERIC_DIRECTIVE);
    if (!r) r = consumeToken(b, EQU_DIRECTIVE);
    if (!r) r = consumeToken(b, SET_DIRECTIVE);
    if (!r) r = consumeToken(b, DEF_DIRECTIVE);
    if (!r) r = consumeToken(b, DB_DIRECTIVE);
    if (!r) r = consumeToken(b, REPORT_DIRECTIVE);
    return r;
  }

  /* ********************************************************** */
  // EQU_DIRECTIVE const EQUAL expression
  static boolean equ_directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "equ_directive")) return false;
    if (!nextTokenIs(b, EQU_DIRECTIVE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EQU_DIRECTIVE);
    r = r && const_$(b, l + 1);
    r = r && consumeToken(b, EQUAL);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // call | operand
  public static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION, "<expression>");
    r = call(b, l + 1);
    if (!r) r = operand(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // GENERIC_DIRECTIVE (<<list_of expression>> | EQUAL expression)?
  static boolean generic_directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_directive")) return false;
    if (!nextTokenIs(b, GENERIC_DIRECTIVE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, GENERIC_DIRECTIVE);
    r = r && generic_directive_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (<<list_of expression>> | EQUAL expression)?
  private static boolean generic_directive_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_directive_1")) return false;
    generic_directive_1_0(b, l + 1);
    return true;
  }

  // <<list_of expression>> | EQUAL expression
  private static boolean generic_directive_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_directive_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = list_of(b, l + 1, AvrAsmParser::expression);
    if (!r) r = generic_directive_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EQUAL expression
  private static boolean generic_directive_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_directive_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EQUAL);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // FUNC <<parenthesized <<list_of expression>> >>
  static boolean generic_funk(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_funk")) return false;
    if (!nextTokenIs(b, FUNC)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FUNC);
    r = r && parenthesized(b, l + 1, generic_funk_1_0_parser_);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // MNEMONIC <<max_two_list argument>>?
  static boolean generic_instruction(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_instruction")) return false;
    if (!nextTokenIs(b, MNEMONIC)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MNEMONIC);
    r = r && generic_instruction_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<max_two_list argument>>?
  private static boolean generic_instruction_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "generic_instruction_1")) return false;
    max_two_list(b, l + 1, AvrAsmParser::argument);
    return true;
  }

  /* ********************************************************** */
  // INDIRECT (MINUS | PLUS)
  public static boolean indirect_postfix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "indirect_postfix")) return false;
    if (!nextTokenIs(b, INDIRECT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, INDIRECT);
    r = r && indirect_postfix_1(b, l + 1);
    exit_section_(b, m, INDIRECT_POSTFIX, r);
    return r;
  }

  // MINUS | PLUS
  private static boolean indirect_postfix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "indirect_postfix_1")) return false;
    boolean r;
    r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, PLUS);
    return r;
  }

  /* ********************************************************** */
  // (MINUS | PLUS) INDIRECT
  public static boolean indirect_prefix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "indirect_prefix")) return false;
    if (!nextTokenIs(b, "<indirect prefix>", MINUS, PLUS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, INDIRECT_PREFIX, "<indirect prefix>");
    r = indirect_prefix_0(b, l + 1);
    r = r && consumeToken(b, INDIRECT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // MINUS | PLUS
  private static boolean indirect_prefix_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "indirect_prefix_0")) return false;
    boolean r;
    r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, PLUS);
    return r;
  }

  /* ********************************************************** */
  // branch_instruction | generic_instruction
  public static boolean instruction(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instruction")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, INSTRUCTION, "<instruction>");
    r = branch_instruction(b, l + 1);
    if (!r) r = generic_instruction(b, l + 1);
    exit_section_(b, l, m, r, false, AvrAsmParser::recovery);
    return r;
  }

  /* ********************************************************** */
  static Parser list_of_$(Parser _element) {
    return (b, l) -> list_of(b, l + 1, _element);
  }

  // <<element>> ( COMMA <<element>> )*
  static boolean list_of(PsiBuilder b, int l, Parser _element) {
    if (!recursion_guard_(b, l, "list_of")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = _element.parse(b, l);
    r = r && list_of_1(b, l + 1, _element);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( COMMA <<element>> )*
  private static boolean list_of_1(PsiBuilder b, int l, Parser _element) {
    if (!recursion_guard_(b, l, "list_of_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!list_of_1_0(b, l + 1, _element)) break;
      if (!empty_element_parsed_guard_(b, "list_of_1", c)) break;
    }
    return true;
  }

  // COMMA <<element>>
  private static boolean list_of_1_0(PsiBuilder b, int l, Parser _element) {
    if (!recursion_guard_(b, l, "list_of_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && _element.parse(b, l);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // <<element>> ( COMMA <<element>> )?
  static boolean max_two_list(PsiBuilder b, int l, Parser _element) {
    if (!recursion_guard_(b, l, "max_two_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = _element.parse(b, l);
    r = r && max_two_list_1(b, l + 1, _element);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( COMMA <<element>> )?
  private static boolean max_two_list_1(PsiBuilder b, int l, Parser _element) {
    if (!recursion_guard_(b, l, "max_two_list_1")) return false;
    max_two_list_1_0(b, l + 1, _element);
    return true;
  }

  // COMMA <<element>>
  private static boolean max_two_list_1_0(PsiBuilder b, int l, Parser _element) {
    if (!recursion_guard_(b, l, "max_two_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && _element.parse(b, l);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // REGISTER | indirect_postfix | indirect_prefix | INDIRECT
  public static boolean memory(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "memory")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MEMORY, "<memory>");
    r = consumeToken(b, REGISTER);
    if (!r) r = indirect_postfix(b, l + 1);
    if (!r) r = indirect_prefix(b, l + 1);
    if (!r) r = consumeToken(b, INDIRECT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // MNEMONIC | BR_MNEMONIC
  static boolean mnemonics(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mnemonics")) return false;
    if (!nextTokenIs(b, "", BR_MNEMONIC, MNEMONIC)) return false;
    boolean r;
    r = consumeToken(b, MNEMONIC);
    if (!r) r = consumeToken(b, BR_MNEMONIC);
    return r;
  }

  /* ********************************************************** */
  // MINUS? (INTEGER | CHAR)
  public static boolean number(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "number")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NUMBER, "<number>");
    r = number_0(b, l + 1);
    r = r && number_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // MINUS?
  private static boolean number_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "number_0")) return false;
    consumeToken(b, MINUS);
    return true;
  }

  // INTEGER | CHAR
  private static boolean number_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "number_1")) return false;
    boolean r;
    r = consumeToken(b, INTEGER);
    if (!r) r = consumeToken(b, CHAR);
    return r;
  }

  /* ********************************************************** */
  // PC | number | symbol
  public static boolean operand(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "operand")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, OPERAND, "<operand>");
    r = consumeToken(b, PC);
    if (!r) r = number(b, l + 1);
    if (!r) r = symbol(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // L_PAREN <<param>> R_PAREN
  static boolean parenthesized(PsiBuilder b, int l, Parser _param) {
    if (!recursion_guard_(b, l, "parenthesized")) return false;
    if (!nextTokenIs(b, L_PAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, L_PAREN);
    r = r && _param.parse(b, l);
    r = r && consumeToken(b, R_PAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // MACROS_NAME | (PREPROCESSOR_NAME (expression)*)
  public static boolean preprocessor(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "preprocessor")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PREPROCESSOR, "<preprocessor>");
    r = consumeToken(b, MACROS_NAME);
    if (!r) r = preprocessor_1(b, l + 1);
    exit_section_(b, l, m, r, false, AvrAsmParser::recovery);
    return r;
  }

  // PREPROCESSOR_NAME (expression)*
  private static boolean preprocessor_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "preprocessor_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PREPROCESSOR_NAME);
    r = r && preprocessor_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (expression)*
  private static boolean preprocessor_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "preprocessor_1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!preprocessor_1_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "preprocessor_1_1", c)) break;
    }
    return true;
  }

  // (expression)
  private static boolean preprocessor_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "preprocessor_1_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(PREPROCESSOR_NAME | MACROS_NAME | LABEL | directives | mnemonics | LINE_COMMENT)
  static boolean recovery(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recovery")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recovery_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // PREPROCESSOR_NAME | MACROS_NAME | LABEL | directives | mnemonics | LINE_COMMENT
  private static boolean recovery_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recovery_0")) return false;
    boolean r;
    r = consumeToken(b, PREPROCESSOR_NAME);
    if (!r) r = consumeToken(b, MACROS_NAME);
    if (!r) r = consumeToken(b, LABEL);
    if (!r) r = directives(b, l + 1);
    if (!r) r = mnemonics(b, l + 1);
    if (!r) r = consumeToken(b, LINE_COMMENT);
    return r;
  }

  /* ********************************************************** */
  // REPORT_DIRECTIVE STRING
  static boolean report_directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "report_directive")) return false;
    if (!nextTokenIs(b, REPORT_DIRECTIVE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, REPORT_DIRECTIVE, STRING);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // SET_DIRECTIVE variable EQUAL expression
  static boolean set_directive(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "set_directive")) return false;
    if (!nextTokenIs(b, SET_DIRECTIVE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SET_DIRECTIVE);
    r = r && variable(b, l + 1);
    r = r && consumeToken(b, EQUAL);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // STRLEN_FUNC <<parenthesized (STRING)>>
  static boolean strlen(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "strlen")) return false;
    if (!nextTokenIs(b, STRLEN_FUNC)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, STRLEN_FUNC);
    r = r && parenthesized(b, l + 1, strlen_1_0_parser_);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER | SYMBOL_IDENTIFIER
  public static boolean symbol(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "symbol")) return false;
    if (!nextTokenIs(b, "<symbol>", IDENTIFIER, SYMBOL_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SYMBOL, "<symbol>");
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, SYMBOL_IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // symbol
  public static boolean variable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable")) return false;
    if (!nextTokenIs(b, "<variable>", IDENTIFIER, SYMBOL_IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VARIABLE, "<variable>");
    r = symbol(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  static final Parser strlen_1_0_parser_ = (b, l) -> consumeToken(b, STRING);

  private static final Parser generic_funk_1_0_parser_ = list_of_$(AvrAsmParser::expression);
}
