package org.jetbrains.tinygoplugin.injection;

import com.goide.completion.GoCompletionUtil;
import com.goide.psi.GoArgumentList;
import com.goide.psi.GoCallExpr;
import com.goide.psi.GoReferenceExpression;
import com.goide.psi.GoStringLiteral;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TinyGoInjectionPatterns extends PlatformPatterns {
    public static PsiElementPattern.@NotNull Capture<GoStringLiteral> tinyGoInlineAssembly(String device) {
        return psiElement(GoStringLiteral.class)
                .withAncestor(1, psiElement(GoArgumentList.class))
                .withAncestor(2, asmInjectionFunctionCall(device));
    }

    private static PsiElementPattern.@NotNull Capture<GoCallExpr> asmInjectionFunctionCall(String device) {
        return psiElement(GoCallExpr.class)
                .withChild(asmInjectionFunctionReference(device));
    }

    private static PsiElementPattern.@NotNull Capture<GoReferenceExpression> asmInjectionFunctionReference(String device) {
        return psiElement(GoReferenceExpression.class)
                .with(GoCompletionUtil.condition("string value is assembly injection function name", it -> {
                    var identifier = (it).getIdentifier().getText();
                    return identifier.equals("Asm") || identifier.equals("AsmFull");
                })).withChild(
                        supportedDeviceLibraryReference(device)
                );
    }

    private static PsiElementPattern.@NotNull Capture<GoReferenceExpression> supportedDeviceLibraryReference(String device) {
        return new PsiElementPattern.Capture<>(GoReferenceExpression.class) {
            public boolean accepts(@Nullable Object o, ProcessingContext context) {
                if (o == null) return false;
                var packageName = ((GoReferenceExpression) o).getText();
                return packageName.equals(device);
            }
        };
    }
}