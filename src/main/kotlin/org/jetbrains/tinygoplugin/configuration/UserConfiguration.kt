package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xmlb.XmlSerializerUtil

interface UserConfiguration {
    var tinyGoSDKPath: String
}

data class UserConfigurationState(override var tinyGoSDKPath: String = "/") : UserConfiguration

@State(name = "TinyGoPluginUserConfig", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
internal class UserConfigurationImpl : PersistentStateComponent<UserConfigurationState> {
    var userConfigurationState = UserConfigurationState()

    override fun getState(): UserConfigurationState = userConfigurationState

    override fun loadState(state: UserConfigurationState) {
        XmlSerializerUtil.copyBean(state, this.userConfigurationState)
    }
}
