package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionBase
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoGoStatement
import com.goide.psi.GoVisitor
import com.goide.util.GoExecutor
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.services.TinyGoInfoExtractor
import org.jetbrains.tinygoplugin.services.extractTinyGoInfo
import org.jetbrains.tinygoplugin.services.propagateGoFlags

class TinyGoStatementInspection : GoInspectionBase() {
    companion object {
        const val GO_INSPECTION_NAME = "inspection.go.statement.name"
        const val GO_INSPECTION_MESSAGE = "inspection.go.statement.message"
        val QUICK_FIX = object : LocalQuickFix {
            override fun getFamilyName(): String = TinyGoBundle.message(GO_INSPECTION_NAME)

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val settings = TinyGoConfiguration.getInstance(project)
                settings.scheduler = Scheduler.AUTO_DETECT
                TinyGoInfoExtractor(project)
                    .extractTinyGoInfo(settings) { _: GoExecutor.ExecutionResult?, output: String ->
                        settings.extractTinyGoInfo(output)
                        settings.saveState(project)
                        propagateGoFlags(project, settings)
                        settings.saveState(project)
                    }
            }
        }
    }

    override fun buildGoVisitor(
        problemsHolder: GoProblemsHolder,
        locationInspection: LocalInspectionToolSession,
    ): GoVisitor =
        object : GoVisitor() {
            override fun visitGoStatement(goStatement: GoGoStatement) {
                super.visitGoStatement(goStatement)
                val project = goStatement.project
                val settings = TinyGoConfiguration.getInstance(project)
                if (settings.enabled && settings.scheduler == Scheduler.NONE) {
                    problemsHolder.registerProblem(
                        goStatement,
                        inspectionMessage(GO_INSPECTION_MESSAGE),
                        ProblemHighlightType.GENERIC_ERROR,
                        QUICK_FIX
                    )
                }
            }
        }
}
