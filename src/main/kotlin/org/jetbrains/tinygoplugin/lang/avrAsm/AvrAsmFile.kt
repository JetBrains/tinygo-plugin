package org.jetbrains.tinygoplugin.lang.avrAsm

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class AvrAsmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AvrAsmLanguage.INSTANCE) {
    override fun getFileType(): FileType = AvrAsmFileType
}
