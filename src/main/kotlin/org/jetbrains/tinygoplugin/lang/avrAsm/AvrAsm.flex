package org.jetbrains.tinygoplugin.lang.avrAsm;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import org.jetbrains.tinygoplugin.lang.avrAsm.psi.AvrAsmTypes;

%%

%class AvrAsmLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \t\f]
EOL_COMMENT=([;]|"//")[^\r\n]*
BLOCK_COMMENT="/*"(~"*/"|([^"*"]|"*"[^"/"])*)

SYMBOL_CHAR = [\w_.$]
IDENTIFIER = [\w_--\d][\w_]*
SYMBOL_IDENTIFIER = [\w_.--\d]{SYMBOL_CHAR}*

BR_PREFIX = ("br"|"BR")
JMP_POSTFIX = ("jmp"|"JMP")

DEC_DIGIT = [0-9]
OCT_DIGIT = [0-7]
HEX_DIGIT = [0-9A-Fa-f]

INTEGER = 0|[1-9]{DEC_DIGIT}*|0{OCT_DIGIT}+|(\$|0[Xx]){HEX_DIGIT}+

ESCAPE = \n | \r | \a | \b | \f | \t | \v | \\ | \0
CHAR = \'[^\']\'
STRING = \"[^\"]*\"

INDIRECT = X | Y | Z
REGISTER = r{DEC_DIGIT}+ | R{DEC_DIGIT}+

%state IN_OPERANDS
%state IN_PREPROCESSOR

%%

<YYINITIAL> ^ !((.*([#;]|"//"|"/*"|{CRLF}))+.*) ":"$        {                             return AvrAsmTypes.LABEL; }
<YYINITIAL> {SYMBOL_CHAR}+ :                                {                             return AvrAsmTypes.LABEL; }
<YYINITIAL> (".set"|".SET")                                 { yybegin(IN_OPERANDS);       return AvrAsmTypes.SET_DIRECTIVE; }
<YYINITIAL> (".equ"|".EQU")                                 { yybegin(IN_OPERANDS);       return AvrAsmTypes.EQU_DIRECTIVE; }
<YYINITIAL> (".def"|".DEF")                                 { yybegin(IN_OPERANDS);       return AvrAsmTypes.DEF_DIRECTIVE; }
<YYINITIAL> (".message"|".MESSAGE")                         { yybegin(IN_OPERANDS);       return AvrAsmTypes.REPORT_DIRECTIVE; }
<YYINITIAL> (".warning"|".WARNING")                         { yybegin(IN_OPERANDS);       return AvrAsmTypes.REPORT_DIRECTIVE; }
<YYINITIAL> (".error"|".ERROR")                             { yybegin(IN_OPERANDS);       return AvrAsmTypes.REPORT_DIRECTIVE; }
<YYINITIAL> "." {SYMBOL_CHAR}+                              { yybegin(IN_OPERANDS);       return AvrAsmTypes.GENERIC_DIRECTIVE; }
<YYINITIAL> "#" {SYMBOL_CHAR}+                              { yybegin(IN_OPERANDS);       return AvrAsmTypes.PREPROCESSOR_NAME; }
<YYINITIAL> "__" ({SYMBOL_CHAR}+) "__"                      {                             return AvrAsmTypes.MACROS_NAME; }
<YYINITIAL> {BR_PREFIX} {SYMBOL_CHAR}+                      { yybegin(IN_OPERANDS);       return AvrAsmTypes.BR_MNEMONIC; }
<YYINITIAL> {SYMBOL_CHAR}* {JMP_POSTFIX}                    { yybegin(IN_OPERANDS);       return AvrAsmTypes.BR_MNEMONIC; }
<YYINITIAL> [\w_.$--\d] {SYMBOL_CHAR}*                      { yybegin(IN_OPERANDS);       return AvrAsmTypes.MNEMONIC; }

{WHITE_SPACE}*[\r\n]+                                       { yybegin(YYINITIAL);      return TokenType.WHITE_SPACE; }
{WHITE_SPACE}+ / .*                                         {                          return TokenType.WHITE_SPACE; }
{EOL_COMMENT}                                               { yybegin(YYINITIAL);      return AvrAsmTypes.LINE_COMMENT; }
{BLOCK_COMMENT} / .*                                        {                          return AvrAsmTypes.BLOCK_COMMENT; }

<IN_OPERANDS> {
("pc"|"PC")                                                 { return AvrAsmTypes.PC; }
{REGISTER}                                                  { return AvrAsmTypes.REGISTER; }
{INDIRECT}                                                  { return AvrAsmTypes.INDIRECT; }
{INTEGER}                                                   { return AvrAsmTypes.INTEGER; }
{CHAR}                                                      { return AvrAsmTypes.CHAR; }
{STRING}                                                    { return AvrAsmTypes.STRING; }

("low"|"LOW")                                               { return AvrAsmTypes.FUNC; }
("high"|"HIGH")                                             { return AvrAsmTypes.FUNC; }
("byte3"|"BYTE3")                                           { return AvrAsmTypes.FUNC; }
("byte4"|"BYTE4")                                           { return AvrAsmTypes.FUNC; }
("lwrd"|"LWRD")                                             { return AvrAsmTypes.FUNC; }
("hwrd"|"HWRD")                                             { return AvrAsmTypes.FUNC; }
("page"|"PAGE")                                             { return AvrAsmTypes.FUNC; }
("exp2"|"EXP2")                                             { return AvrAsmTypes.FUNC; }
("log2"|"LOG2")                                             { return AvrAsmTypes.FUNC; }
("int"|"INT")                                               { return AvrAsmTypes.FUNC; }
("frac"|"FRAC")                                             { return AvrAsmTypes.FUNC; }
("q7"|"Q7")                                                 { return AvrAsmTypes.FUNC; }
("abs"|"ABS")                                               { return AvrAsmTypes.FUNC; }
("defined"|"DEFINED")                                       { return AvrAsmTypes.DEFINED_FUNC; }
("strlen"|"STRLEN")                                         { return AvrAsmTypes.STRLEN_FUNC; }

{IDENTIFIER}                                                { return AvrAsmTypes.IDENTIFIER; }
{SYMBOL_IDENTIFIER}                                         { return AvrAsmTypes.SYMBOL_IDENTIFIER; }

"!"                                                         { return AvrAsmTypes.NOT; }
"~"                                                         { return AvrAsmTypes.TILDA; }
"*"                                                         { return AvrAsmTypes.STAR; }
"/"                                                         { return AvrAsmTypes.DIVISION; }
//<IN_OPERANDS> "%"                                           { return AvrAsmTypes.MODULO; }*/
"+"                                                         { return AvrAsmTypes.PLUS; }
"-"                                                         { return AvrAsmTypes.MINUS; }
"<<"                                                        { return AvrAsmTypes.SHIFT_LEFT; }
">>"                                                        { return AvrAsmTypes.SHIFT_RIGHT; }
"<"                                                         { return AvrAsmTypes.LESS; }
"<="                                                        { return AvrAsmTypes.LESS_EQUAL; }
">"                                                         { return AvrAsmTypes.GREATER; }
">="                                                        { return AvrAsmTypes.GREATER_EQUAL; }
"=="                                                        { return AvrAsmTypes.EQUAL; }
"!="                                                        { return AvrAsmTypes.NOT_EQUAL; }
"&"                                                         { return AvrAsmTypes.AND_BIN; }
"^"                                                         { return AvrAsmTypes.XOR_BIN; }
"|"                                                         { return AvrAsmTypes.OR_BIN; }
"&&"                                                        { return AvrAsmTypes.AND; }
"||"                                                        { return AvrAsmTypes.OR; }

","                                                         { return AvrAsmTypes.COMMA; }
"("                                                         { return AvrAsmTypes.L_PAREN; }
")"                                                         { return AvrAsmTypes.R_PAREN; }
"="                                                         { return AvrAsmTypes.EQUAL; }
}

.                                                           { return TokenType.BAD_CHARACTER; }
