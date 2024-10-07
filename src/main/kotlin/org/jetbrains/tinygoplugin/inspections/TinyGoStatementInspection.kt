package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoGoStatement
import com.goide.psi.GoVisitor
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService

class TinyGoStatementInspection : GoInspectionBase() {
    override fun buildVisitor(
        problemsHolder: GoProblemsHolder,
        locationInspection: LocalInspectionToolSession,
    ): GoVisitor =
        object : GoVisitor() {
            override fun visitGoStatement(goStatement: GoGoStatement) {
                super.visitGoStatement(goStatement)
                val project = goStatement.project
                val settings = project.tinyGoConfiguration()
                if (settings.enabled && settings.scheduler == Scheduler.NONE) {
                    problemsHolder.registerProblem(
                        goStatement,
                        inspectionMessage("inspection.go.statement.message"),
                        ProblemHighlightType.GENERIC_ERROR,
                        EDIT_SETTINGS_QUICK_FIX
                    )
                }
            }
        }
}

private val EDIT_SETTINGS_QUICK_FIX = object : LocalQuickFix {
    override fun getFamilyName(): String = TinyGoBundle.message("inspection.go.statement.edit.settings.fix")

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        invokeLater {
            service<ShowSettingsUtil>().editConfigurable(project, TinyGoSettingsService(project))
        }
    }
}
