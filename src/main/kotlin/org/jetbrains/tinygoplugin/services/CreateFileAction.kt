package org.jetbrains.tinygoplugin.services

import com.goide.execution.GoRunUtil
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.rd.util.ConcurrentHashMap
import org.jetbrains.tinygoplugin.TinyGoBundle.message
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import org.jetbrains.tinygoplugin.sdk.nullSdk
import java.util.concurrent.ConcurrentMap

private const val TINYGO_FILE_TEMPLATE = "ui.template.name"
private const val TINYGO_TEMPLATE_ACTION_NAME = "ui.template.action.name"
private const val TINYGO_TEMPLATE_DIALOG_NAME = "ui.template.dialog.name"

class CreateFileAction :
    CreateFileFromTemplateAction(
        message(TINYGO_FILE_TEMPLATE),
        "",
        TinyGoPluginIcons.TinyGoIcon
    ) {

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        if (tinyGoSettings.sdk != nullSdk) {

            builder.setTitle(message(TINYGO_TEMPLATE_DIALOG_NAME))
            val examples = availableExamples(project, tinyGoSettings.sdk.sdkRoot!!)
            examples.forEach {
                builder.addKind(it, TinyGoPluginIcons.TinyGoIcon, it)
            }
        }
    }

    private fun availableExamples(project: Project, sdkPath: VirtualFile): Collection<String> {
        val examples = sdkPath.findChild("src")?.findChild("examples") ?: return emptyList()
        val psiManager = PsiManager.getInstance(project)
        val result = HashSet<String>()
        VfsUtil.visitChildrenRecursively(examples, object : VirtualFileVisitor<Unit>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.extension == "go") {
                    val psiFile = psiManager.findFile(file)
                    if (GoRunUtil.isMainGoFile(psiFile)) {
                        result.add(VfsUtil.getRelativePath(file, examples) ?: "")
                    }
                }
                return true
            }
        })
        return result.filter(String::isNotEmpty)
    }
    @Suppress("ReturnCount")
    override fun createFile(name: String?, templateName: String?, dir: PsiDirectory?): PsiFile? {
        val project = dir?.project ?: return null
        val sdkPath = TinyGoConfiguration.getInstance(project).sdk.sdkRoot ?: return null
        val examples = VfsUtil.findRelativeFile(sdkPath, "src", "examples")
        if (templateName == null || templateName.isEmpty()) {
            return null
        }
        val exampleFile = examples?.findFileByRelativePath(templateName) ?: return null
        if (name == null || name.isEmpty()) {
            return null
        }
        var filename = name
        if (!name.endsWith(".go")) {
            filename = "$name.go"
        }
        val result = VfsUtil.copyFile(project, exampleFile, dir.virtualFile, filename)
        val psiManager = PsiManager.getInstance(project)
        return psiManager.findFile(result)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return message(TINYGO_TEMPLATE_ACTION_NAME)
    }
}
