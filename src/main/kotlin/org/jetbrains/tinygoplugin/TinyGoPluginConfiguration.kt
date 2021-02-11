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
    AUTO_DETECT("Auto detect"),
    NONE("none"),
    LEAKING("leaking"),
    EXTALLOC("extalloc"),
    CONSERVATIVE("conservative")
}

enum class Scheduler(val cmd: String) {
    AUTO_DETECT("Auto detect"),
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
    var goOS: String
}

@State(name = "TinyGoPluginUserConfig", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
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

@State(name = "TinyGoPlugin", storages = [Storage("tinygoSettings.xml")])
@Service(Service.Level.PROJECT)
class ProjectConfigurationImpl :
    PersistentStateComponent<ProjectConfigurationImpl>, ProjectConfiguration {
    override var gc = GarbageCollector.AUTO_DETECT
    override var scheduler = Scheduler.AUTO_DETECT
    override var targetPlatform = ""
    override var goTags = ""
    override var goArch = ""
    override var goOS = ""

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
