package org.jetbrains.tinygoplugin

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File


enum class GarbageCollector {
    NONE, LEAKING, EXTALLOC, CONSERVATIVE
}

enum class Scheduler {
    NONE, COROUTINES, TASKS
}


interface UserConfiguration {

    var tinyGoSDKPath: File
}

interface ProjectConfiguration {
    var gc: GarbageCollector
    var scheduler: Scheduler
    var targetPlatform: String
}

@State(name = "TinyGoPluginUserConfig", storages = [Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class UserConfigurationImpl : PersistentStateComponent<UserConfigurationImpl>, UserConfiguration {
    override var tinyGoSDKPath: File = File("")

    override fun getState(): UserConfigurationImpl {
        return this
    }

    override fun loadState(state: UserConfigurationImpl) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

@State(name = "TinyGoPlugin", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class ProjectConfigurationImpl :
    PersistentStateComponent<ProjectConfigurationImpl>, ProjectConfiguration {
    override var gc = GarbageCollector.NONE
    override var scheduler = Scheduler.COROUTINES
    override var targetPlatform = ""


    override fun getState(): ProjectConfigurationImpl {
        return this
    }

    override fun loadState(state: ProjectConfigurationImpl) {
        XmlSerializerUtil.copyBean(state, this)
    }
}

class TinyGoConfiguration(userConfig: UserConfiguration, projectConfig: ProjectConfiguration) :
    UserConfiguration by userConfig, ProjectConfiguration by projectConfig {

    companion object {
        fun getInstance(p: Project): TinyGoConfiguration = TinyGoConfiguration(
            projectConfig = p.service<ProjectConfigurationImpl>(),
            userConfig = p.service<UserConfigurationImpl>()
        )
    }
}


