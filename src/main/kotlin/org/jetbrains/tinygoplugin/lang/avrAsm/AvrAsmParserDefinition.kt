package org.jetbrains.tinygoplugin.lang.avrAsm

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.tinygoplugin.lang.avrAsm.parser.AvrAsmParser
import org.jetbrains.tinygoplugin.lang.avrAsm.psi.AvrAsmTypes

private val FILE = IFileElementType(AvrAsmLanguage.INSTANCE)
private val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
private val COMMENTS = TokenSet.create(AvrAsmTypes.LINE_COMMENT, AvrAsmTypes.BLOCK_COMMENT)
private val STRINGS = TokenSet.create(AvrAsmTypes.STRING)

class AvrAsmParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = AvrAsmLexerAdapter()

    override fun createParser(project: Project?): PsiParser = AvrAsmParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode?): PsiElement = AvrAsmTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = AvrAsmFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements =
        ParserDefinition.SpaceRequirements.MAY
}
