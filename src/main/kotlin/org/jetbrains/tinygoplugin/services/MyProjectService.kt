package org.jetbrains.tinygoplugin.services

import org.jetbrains.tinygoplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
