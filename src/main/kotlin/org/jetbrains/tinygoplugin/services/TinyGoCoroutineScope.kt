package org.jetbrains.tinygoplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

object TinyGoServiceScope {
    fun getScope(project: Project?) = project?.let { project.service<TinyGoServiceProjectScope>().scope }
        ?: getScope()
    fun getScope() = service<TinyGoServiceApplicationScope>().scope
}

@Service(Service.Level.APP)
class TinyGoServiceApplicationScope(val scope: CoroutineScope)

@Service(Service.Level.PROJECT)
class TinyGoServiceProjectScope(val project: Project, val scope: CoroutineScope)
