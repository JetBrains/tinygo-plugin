// This is a generated file. Not intended for manual editing.
package org.jetbrains.tinygoplugin.lang.avrAsm.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface AvrAsmExpression extends PsiElement {

  @Nullable
  AvrAsmAndExpr getAndExpr();

  @Nullable
  AvrAsmBitwiseExpr getBitwiseExpr();

  @Nullable
  AvrAsmCall getCall();

  @Nullable
  AvrAsmComparisonExpr getComparisonExpr();

  @Nullable
  AvrAsmMulExpr getMulExpr();

  @Nullable
  AvrAsmNumber getNumber();

  @Nullable
  AvrAsmOrExpr getOrExpr();

  @Nullable
  AvrAsmParen getParen();

  @Nullable
  AvrAsmPlusExpr getPlusExpr();

  @Nullable
  AvrAsmSymbol getSymbol();

  @Nullable
  AvrAsmUnary getUnary();

}
