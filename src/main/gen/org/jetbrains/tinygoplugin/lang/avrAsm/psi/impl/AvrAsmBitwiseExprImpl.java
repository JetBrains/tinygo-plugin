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

public class AvrAsmBitwiseExprImpl extends ASTWrapperPsiElement implements AvrAsmBitwiseExpr {

  public AvrAsmBitwiseExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AvrAsmVisitor visitor) {
    visitor.visitBitwiseExpr(this);
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
  public List<AvrAsmCall> getCallList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmCall.class);
  }

  @Override
  @NotNull
  public List<AvrAsmMulExpr> getMulExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmMulExpr.class);
  }

  @Override
  @NotNull
  public List<AvrAsmNumber> getNumberList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmNumber.class);
  }

  @Override
  @Nullable
  public AvrAsmPlusExpr getPlusExpr() {
    return findChildByClass(AvrAsmPlusExpr.class);
  }

  @Override
  @NotNull
  public List<AvrAsmSymbol> getSymbolList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmSymbol.class);
  }

}
