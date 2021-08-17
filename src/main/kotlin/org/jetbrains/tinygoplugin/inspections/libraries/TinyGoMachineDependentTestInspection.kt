package org.jetbrains.tinygoplugin.inspections.libraries

import com.goide.execution.testing.GoTestFinder
import com.goide.inspections.core.GoProblemsHolder
import com.goide.psi.GoFile
import com.goide.psi.GoImportSpec
import com.goide.psi.GoReferenceExpression
import com.goide.psi.GoVisitor
import com.intellij.codeInspection.LocalInspectionToolSession
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration

class TinyGoMachineDependentTestInspection : TinyGoImportInspection() {
    companion object {
        private const val HARDWARE_TEST_INSPECTION_MESSAGE = "inspection.import.machine.test.message"
    }

    override fun buildGoVisitor(
        holder: GoProblemsHolder,
        locationInspection: LocalInspectionToolSession,
    ): GoVisitor {
        return object : GoVisitor() {
            override fun visitReferenceExpression(o: GoReferenceExpression) {
                super.visitReferenceExpression(o)
                if (!TinyGoConfiguration.getInstance(o.project).enabled) {
                    return
                }
                if (GoTestFinder.isTestFile(o.containingFile)) {
                    val target = o.resolve()
                    val file = target?.containingFile
                    if (file is GoFile) {
                        val importPath = file.getImportPath(false) ?: return
                        if (importPath.split("/").first() == "machine") {
                            holder.registerImportProblem(o, HARDWARE_TEST_INSPECTION_MESSAGE)
                        }
                    }
                }
            }

            override fun visitImportSpec(o: GoImportSpec) {
                super.visitImportSpec(o)
                if (o.isCImport || o.isBlank || !TinyGoConfiguration.getInstance(o.project).enabled) {
                    return
                }
                val file = o.containingFile
                if (GoTestFinder.isTestFile(file)) {
                    val importPath = o.path
                    if (importPath.split("/").first() == "machine") {
                        holder.registerImportProblem(o, HARDWARE_TEST_INSPECTION_MESSAGE)
                    }
                }
            }
        }
    }
}
