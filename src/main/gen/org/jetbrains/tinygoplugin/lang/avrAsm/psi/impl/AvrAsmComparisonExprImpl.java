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

public class AvrAsmComparisonExprImpl extends ASTWrapperPsiElement implements AvrAsmComparisonExpr {

  public AvrAsmComparisonExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AvrAsmVisitor visitor) {
    visitor.visitComparisonExpr(this);
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
  public AvrAsmCall getCall() {
    return findChildByClass(AvrAsmCall.class);
  }

  @Override
  @NotNull
  public AvrAsmExpression getExpression() {
    return findNotNullChildByClass(AvrAsmExpression.class);
  }

  @Override
  @Nullable
  public AvrAsmMulExpr getMulExpr() {
    return findChildByClass(AvrAsmMulExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmNumber getNumber() {
    return findChildByClass(AvrAsmNumber.class);
  }

  @Override
  @Nullable
  public AvrAsmParen getParen() {
    return findChildByClass(AvrAsmParen.class);
  }

  @Override
  @Nullable
  public AvrAsmPlusExpr getPlusExpr() {
    return findChildByClass(AvrAsmPlusExpr.class);
  }

  @Override
  @Nullable
  public AvrAsmSymbol getSymbol() {
    return findChildByClass(AvrAsmSymbol.class);
  }

  @Override
  @Nullable
  public AvrAsmUnary getUnary() {
    return findChildByClass(AvrAsmUnary.class);
  }

}
