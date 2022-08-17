// This is a generated file. Not intended for manual editing.
package org.jetbrains.tinygoplugin.lang.avrAsm.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.jetbrains.tinygoplugin.lang.avrAsm.psi.AvrAsmTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.jetbrains.tinygoplugin.lang.avrAsm.psi.*;

public class AvrAsmNumberImpl extends ASTWrapperPsiElement implements AvrAsmNumber {

  public AvrAsmNumberImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AvrAsmVisitor visitor) {
    visitor.visitNumber(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AvrAsmVisitor) accept((AvrAsmVisitor)visitor);
    else super.accept(visitor);
  }

}
