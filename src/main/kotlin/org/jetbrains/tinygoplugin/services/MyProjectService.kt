package org.jetbrains.tinygoplugin.services

import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
