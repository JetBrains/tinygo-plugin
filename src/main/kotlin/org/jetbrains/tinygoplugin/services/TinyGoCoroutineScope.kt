package org.jetbrains.tinygoplugin.services

import com.intellij.execution.process.mediator.util.blockingGet
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

object TinyGoServiceScope {
    fun getScope(project: Project) = project.service<TinyGoServiceProjectScope>().scope
    fun getScope() = service<TinyGoServiceApplicationScope>().scope
}

@Service(Service.Level.APP)
class TinyGoServiceApplicationScope(val scope: CoroutineScope)

@Service(Service.Level.PROJECT)
class TinyGoServiceProjectScope(val project: Project, val scope: CoroutineScope)

fun <T> CoroutineScope.blockingIO(block: suspend CoroutineScope.() -> T): T = async {
    withContext(Dispatchers.IO) {
        block.invoke(this)
    }
}.blockingGet()
