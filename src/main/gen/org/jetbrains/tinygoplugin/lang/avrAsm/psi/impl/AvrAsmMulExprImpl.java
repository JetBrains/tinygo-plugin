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

public class AvrAsmMulExprImpl extends ASTWrapperPsiElement implements AvrAsmMulExpr {

  public AvrAsmMulExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AvrAsmVisitor visitor) {
    visitor.visitMulExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AvrAsmVisitor) accept((AvrAsmVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<AvrAsmCall> getCallList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmCall.class);
  }

  @Override
  @Nullable
  public AvrAsmMulExpr getMulExpr() {
    return findChildByClass(AvrAsmMulExpr.class);
  }

  @Override
  @NotNull
  public List<AvrAsmNumber> getNumberList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmNumber.class);
  }

  @Override
  @NotNull
  public List<AvrAsmSymbol> getSymbolList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AvrAsmSymbol.class);
  }

}
