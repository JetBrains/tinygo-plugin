package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.services.tinygoTargets

class SettingsWithHistory(val settings: TinyGoConfiguration, val project: Project) : TinyGoConfiguration by settings {
    constructor(project: Project) : this(TinyGoConfiguration.getInstance(project).deepCopy(), project)

    override var sdk: TinyGoSdk
        get() = settings.sdk
        set(value) {
            if (value != settings.sdk) {
                settings.sdk = value
                predefinedTargets = tinygoTargets(value).toSet()
            }
        }
    override var targetPlatform: String
        get() {
            if (predefinedTargets.contains(settings.targetPlatform)) {
                return settings.targetPlatform
            }
            val absolutePath = absoluteOrRelativePath(settings.targetPlatform, project)
            return absolutePath?.path ?: settings.targetPlatform
        }
        set(value) {
            if (!predefinedTargets.contains(value)) {
                var relativeTarget = toRelativePath(value, project)
                if (relativeTarget.isNotEmpty()) {
                    relativeTarget = value
                }
                if (!userTargets.contains(relativeTarget)) {
                    settings.userTargets = userTargets + relativeTarget
                }
            } else {
                settings.targetPlatform = value
            }
        }
    override var userTargets: List<String>
        get() = settings.userTargets.mapNotNull { absoluteOrRelativePath(it, project) }
            .map { it.path } + predefinedTargets.toList()
        set(value) {
            settings.userTargets = value
        }
    var predefinedTargets: Set<String> = tinygoTargets(settings.sdk).toSet()
}

fun absoluteOrRelativePath(path: String, project: Project?): VirtualFile? {
    val pathUrl = VfsUtil.convertToURL(path) ?: return null
    var result = VfsUtil.findFileByURL(pathUrl)
    if (result == null && project != null) {
        val projectPath = project.basePath
        val absolutePath = FileUtil.join(projectPath, path)
        val absoluteUrl = VfsUtil.convertToURL(absolutePath) ?: return null
        result = VfsUtil.findFileByURL(absoluteUrl)
    }
    return result
}

fun toRelativePath(path: String, project: Project): String {
    val projectPath = project.basePath ?: return ""
    if (!path.startsWith(projectPath)) {
        return ""
    }
    return path.substring(projectPath.length)
}
