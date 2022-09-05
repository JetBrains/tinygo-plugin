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

public class AvrAsmUnaryImpl extends ASTWrapperPsiElement implements AvrAsmUnary {

  public AvrAsmUnaryImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AvrAsmVisitor visitor) {
    visitor.visitUnary(this);
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
  @Nullable
  public AvrAsmLiteralExpr getLiteralExpr() {
    return findChildByClass(AvrAsmLiteralExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmMulExpr getMulExpr() {
    return findChildByClass(AvrAsmMulExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmParenExpr getParenExpr() {
    return findChildByClass(AvrAsmParenExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmPlusExpr getPlusExpr() {
    return findChildByClass(AvrAsmPlusExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmRefExpr getRefExpr() {
    return findChildByClass(AvrAsmRefExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmUnary getUnary() {
    return findChildByClass(AvrAsmUnary.class);
  }

}
