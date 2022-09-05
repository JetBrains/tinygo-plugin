// This is a generated file. Not intended for manual editing.
package org.jetbrains.tinygoplugin.lang.avrAsm.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface AvrAsmAndExpr extends PsiElement {

  @Nullable
  AvrAsmBitwiseExpr getBitwiseExpr();

  @Nullable
  AvrAsmCall getCall();

  @NotNull
  AvrAsmExpression getExpression();

  @Nullable
  AvrAsmMulExpr getMulExpr();

  @Nullable
  AvrAsmNumber getNumber();

  @Nullable
  AvrAsmParen getParen();

  @Nullable
  AvrAsmPlusExpr getPlusExpr();

  @Nullable
  AvrAsmSymbol getSymbol();

  @Nullable
  AvrAsmUnary getUnary();

}
