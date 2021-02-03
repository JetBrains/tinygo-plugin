package com.github.pleomaxx3002.tinygoplugin.services

import com.github.pleomaxx3002.tinygoplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
