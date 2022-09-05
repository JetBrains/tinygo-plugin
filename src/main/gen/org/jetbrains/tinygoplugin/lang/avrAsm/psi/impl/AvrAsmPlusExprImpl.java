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

public class AvrAsmPlusExprImpl extends ASTWrapperPsiElement implements AvrAsmPlusExpr {

  public AvrAsmPlusExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AvrAsmVisitor visitor) {
    visitor.visitPlusExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AvrAsmVisitor) accept((AvrAsmVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public AvrAsmBitwiseExpr getBitwiseExpr() {
    return findChildByClass(AvrAsmBitwiseExpr.class);
  }

  @Override
  @NotNull
  public List<AvrAsmLiteralExpr> getLiteralExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmLiteralExpr.class);
  }

  @Override
  @NotNull
  public List<AvrAsmMulExpr> getMulExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmMulExpr.class);
  }

  @Override
  @NotNull
  public List<AvrAsmParenExpr> getParenExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmParenExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmPlusExpr getPlusExpr() {
    return findChildByClass(AvrAsmPlusExpr.class);
  }

  @Override
  @NotNull
  public List<AvrAsmRefExpr> getRefExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmRefExpr.class);
  }

}
