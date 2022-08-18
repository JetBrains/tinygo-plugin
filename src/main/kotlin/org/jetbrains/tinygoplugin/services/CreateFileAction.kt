package org.jetbrains.tinygoplugin.services

import com.goide.execution.GoRunUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.tinygoplugin.TinyGoBundle.message
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import org.jetbrains.tinygoplugin.sdk.notifyTinyGoNotConfigured
import org.jetbrains.tinygoplugin.sdk.nullSdk

private const val TINYGO_FILE_TEMPLATE = "ui.template.name"
private const val TINYGO_TEMPLATE_ACTION_NAME = "ui.template.action.name"
private const val TINYGO_TEMPLATE_DIALOG_NAME = "ui.template.dialog.name"
private const val TINYGO_INVALID_SDK = "notifications.tinyGoSdk.tinyGoSDKInvalid"
private const val TINYGO_TEMPLATES_NOT_FOUND_TITLE = "file.create.errors.templatesCannotLoad.title"
private const val TINYGO_TEMPLATES_NOT_FOUND_MESSAGE = "file.create.errors.templatesCannotLoad.message"
private const val TINYGO_FILE_EXISTS_TITLE = "file.create.errors.fileExists.title"
private const val TINYGO_FILE_EXISTS_MESSAGE = "file.create.errors.fileExists.message"
private const val TINYGO_DIR_NOT_EMPTY_MESSAGE = "file.create.errors.dirNotEmpty.message"

private const val GO_MOD_FILENAME = "go.mod"

class CreateFileAction :
    CreateFileFromTemplateAction(
        message(TINYGO_FILE_TEMPLATE),
        "",
        TinyGoPluginIcons.TinyGoIcon
    ) {
    private val mainFilesInDirs: MutableMap<VirtualFile, String> = hashMapOf()

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        if (tinyGoSettings.sdk != nullSdk) {
            if (!tinyGoSettings.sdk.isValid) {
                Messages.showErrorDialog(
                    project,
                    message(TINYGO_TEMPLATES_NOT_FOUND_MESSAGE),
                    message(TINYGO_TEMPLATES_NOT_FOUND_TITLE)
                )
                notifyTinyGoNotConfigured(project, message(TINYGO_INVALID_SDK))
                return
            }
            builder.setTitle(message(TINYGO_TEMPLATE_DIALOG_NAME))
            val examples = availableExamples(project, tinyGoSettings.sdk.sdkRoot!!)
            examples.files.forEach {
                builder.addKind(it, TinyGoPluginIcons.TinyGoIcon, it)
            }
            examples.dirs.sorted().forEach {
                builder.addKind(it, AllIcons.Nodes.Folder, it)
            }
        }
    }

    private fun availableExamples(project: Project, sdkPath: VirtualFile): ExamplesSearchResult {
        val examples = sdkPath.findChild("src")?.findChild("examples") ?: return ExamplesSearchResult()
        val psiManager = PsiManager.getInstance(project)
        val result = ExamplesSearchResult()
        VfsUtil.visitChildrenRecursively(
            examples,
            object : VirtualFileVisitor<Unit>() {
                private val ignoredDirectories = hashSetOf<VirtualFile>()

                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory && file.extension == "go") {
                        val psiFile = psiManager.findFile(file)
                        if (GoRunUtil.isMainGoFile(psiFile)) {
                            val noOtherFiles = file.parent.children.size == 1
                            if (noOtherFiles) {
                                result.files.addIfNotNull(VfsUtil.getRelativePath(file, examples))
                            } else {
                                val parentDir = file.parent
                                if (parentDir !in ignoredDirectories) {
                                    mainFilesInDirs[parentDir] = file.name
                                    ignoredDirectories.add(parentDir.parent)
                                    result.dirs.addIfNotNull(VfsUtil.getRelativePath(parentDir, examples))
                                }
                            }
                        }
                    }
                    return true
                }
            }
        )
        return result
    }

    inner class ExamplesSearchResult {
        val files: MutableSet<String> = HashSet()
        val dirs: MutableSet<String> = HashSet()
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
        if (dir.findFile(filename) != null) {
            Messages.showErrorDialog(
                project,
                message(TINYGO_FILE_EXISTS_MESSAGE, "${dir.virtualFile.path}/$filename"),
                message(TINYGO_FILE_EXISTS_TITLE)
            )
            return null
        }
        val result = copyExample(project, exampleFile, dir.virtualFile, filename) ?: return null
        val psiManager = PsiManager.getInstance(project)
        return psiManager.findFile(result)
    }

    @Suppress("ReturnCount")
    private fun copyExample(
        project: Project,
        example: VirtualFile,
        dir: VirtualFile,
        filename: String
    ): VirtualFile? {
        return if (example.isDirectory) {
            if (dir.children.any { it.name.substringAfterLast('/') != GO_MOD_FILENAME }) {
                Messages.showErrorDialog(
                    project,
                    message(TINYGO_DIR_NOT_EMPTY_MESSAGE, dir.path),
                    message(TINYGO_FILE_EXISTS_TITLE)
                )
                return null
            }

            VfsUtil.copyDirectory(project, example, dir, null)
            val mainFileName = mainFilesInDirs[example] ?: return null
            val mainFile = dir.findChild(mainFileName) ?: return null
            mainFile.rename(project, filename)
            mainFile
        } else {
            VfsUtil.copyFile(project, example, dir, filename)
        }
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return message(TINYGO_TEMPLATE_ACTION_NAME)
    }
}

private fun MutableSet<String>.addIfNotNull(element: String?) {
    if (element != null) add(element)
}
