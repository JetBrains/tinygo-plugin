package org.jetbrains.tinygoplugin.inspections.libraries

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.tinygoplugin.runconfig.isTestGoFile

class TinyGoMachineDependentTestInspection : TinyGoImportInspection() {
    companion object {
        private const val HARDWARE_TEST_INSPECTION_MESSAGE = "inspection.import.machine.test.message"
    }

    override val defaultInspectionMessage: String = HARDWARE_TEST_INSPECTION_MESSAGE

    override fun getDefinedUnsupportedPackages(project: Project): Set<String> = setOf("machine")

    override fun customContextPredicate(o: PsiElement): Boolean = isTestGoFile(o.containingFile)
}
