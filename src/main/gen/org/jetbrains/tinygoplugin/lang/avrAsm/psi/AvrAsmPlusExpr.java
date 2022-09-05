// This is a generated file. Not intended for manual editing.
package org.jetbrains.tinygoplugin.lang.avrAsm.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface AvrAsmPlusExpr extends PsiElement {

  @Nullable
  AvrAsmBitwiseExpr getBitwiseExpr();

  @NotNull
  List<AvrAsmCall> getCallList();

  @NotNull
  List<AvrAsmMulExpr> getMulExprList();

  @NotNull
  List<AvrAsmNumber> getNumberList();

  @Nullable
  AvrAsmPlusExpr getPlusExpr();

  @NotNull
  List<AvrAsmSymbol> getSymbolList();

}
