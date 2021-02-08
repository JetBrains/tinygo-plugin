package org.jetbrains.tinygoplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

enum class GarbageCollector(val cmd: String) {
    NONE("none"),
    LEAKING("leaking"),
    EXTALLOC("extalloc"),
    CONSERVATIVE("conservative")
}

enum class Scheduler(val cmd: String) {
    NONE("none"),
    COROUTINES("coroutines"),
    TASKS("tasks")
}

interface UserConfiguration {

    var tinyGoSDKPath: String
}

interface ProjectConfiguration {
    var gc: GarbageCollector
    var scheduler: Scheduler
    var targetPlatform: String
    var goTags: String
    var goArch: String
}

@State(name = "TinyGoPluginUserConfig", storages = [Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
class UserConfigurationImpl : PersistentStateComponent<UserConfigurationImpl>, UserConfiguration {
    override var tinyGoSDKPath = ""

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
    override var goTags = ""
    override var goArch = ""

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
