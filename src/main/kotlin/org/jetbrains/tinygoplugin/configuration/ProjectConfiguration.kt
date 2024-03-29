package org.jetbrains.tinygoplugin.configuration

import com.goide.project.GoModuleSettings
import com.intellij.conversion.impl.ConversionContextImpl
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.xmlb.XmlSerializerUtil
import java.nio.file.Path

interface ProjectConfiguration {
    var gc: GarbageCollector
    var scheduler: Scheduler
    var targetPlatform: String
    var goTags: String
    var goArch: String
    var goOS: String
    var userTargets: List<String>
}

data class ProjectConfigurationState(
    override var gc: GarbageCollector = GarbageCollector.AUTO_DETECT,
    override var scheduler: Scheduler = Scheduler.AUTO_DETECT,
    override var targetPlatform: String = "",
    override var goTags: String = "",
    override var goArch: String = "",
    override var goOS: String = "",
    override var userTargets: List<String> = emptyList(),
) : ProjectConfiguration

@State(name = "TinyGoPlugin", storages = [Storage("tinygoSettings.xml")])
@Service(Service.Level.PROJECT)
internal class ProjectConfigurationImpl(val project: Project) :
    PersistentStateComponent<ProjectConfigurationState> {
    var myState = ProjectConfigurationState()
    private val context: ConversionContextImpl

    init {
        val baseDir = project.basePath ?: project.projectFilePath ?: ""
        context = ConversionContextImpl(Path.of(baseDir))

        val connection: MessageBusConnection = project.messageBus.connect()
        connection.subscribe(GoModuleSettings.BUILD_TARGET_TOPIC, CachedGoRootUpdater())
    }

    override fun loadState(state: ProjectConfigurationState) {
        XmlSerializerUtil.copyBean(state, myState)
        myState.targetPlatform = context.expandPath(myState.targetPlatform)
        myState.userTargets = myState.userTargets.filter { it.isNotBlank() }.map(context::collapsePath)
    }

    override fun getState(): ProjectConfigurationState {
        // collapse paths
        return myState.copy(
            targetPlatform = context.collapsePath(myState.targetPlatform),
            userTargets = myState.userTargets.map(context::collapsePath)
        )
    }
}
