package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

interface ProjectConfiguration {
    var gc: GarbageCollector
    var scheduler: Scheduler
    var targetPlatform: String
    var goTags: String
    var goArch: String
    var goOS: String
}

data class ProjectConfigurationState(
    override var gc: GarbageCollector = GarbageCollector.AUTO_DETECT,
    override var scheduler: Scheduler = Scheduler.AUTO_DETECT,
    override var targetPlatform: String = "",
    override var goTags: String = "",
    override var goArch: String = "",
    override var goOS: String = "",
) : ProjectConfiguration

@State(name = "TinyGoPlugin", storages = [Storage("tinygoSettings.xml")])
@Service(Service.Level.PROJECT)
internal class ProjectConfigurationImpl :
    PersistentStateComponent<ProjectConfigurationState> {
    var myState = ProjectConfigurationState()

    override fun loadState(state: ProjectConfigurationState) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    override fun getState(): ProjectConfigurationState = myState
}
