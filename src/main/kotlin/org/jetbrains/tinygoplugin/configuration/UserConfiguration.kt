package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xmlb.XmlSerializerUtil

interface UserConfiguration {
    var tinyGoSDKPath: TinyGoSdk
}

interface UserConfigurationStorage {
    var sdk: TinyGoSdkSerialized
}

data class UserConfigurationState(override var sdk: TinyGoSdkSerialized = TinyGoSdkSerialized("", unknownVersion)) :
    UserConfigurationStorage

fun TinyGoSdk.serialized(): TinyGoSdkSerialized {
    return TinyGoSdkSerialized(this.homeUrl, this.sdkVersion)
}

fun TinyGoSdkSerialized.deserialized(): TinyGoSdk {
    val homeUrl = if (sdkUrl.isEmpty()) null else sdkUrl
    return TinyGoSdk(homeUrl, this.version.toString())
}

class UserConfigurationAPI : UserConfigurationStorage, UserConfiguration {
    internal fun updateState() {
        tinyGoSdk = state.sdk.deserialized()
    }

    override var tinyGoSDKPath: TinyGoSdk
        get() = tinyGoSdk
        set(value) {
            tinyGoSdk = value
            state.sdk = value.serialized()
        }
    private var tinyGoSdk: TinyGoSdk = nullSdk
    var state: UserConfigurationState = UserConfigurationState()
    override var sdk: TinyGoSdkSerialized
        get() = state.sdk
        set(value) {
            state.sdk = value
            tinyGoSdk = state.sdk.deserialized()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserConfigurationAPI) return false
        return state == other.state
    }

    fun copy(): UserConfigurationAPI {
        val result = UserConfigurationAPI()
        result.sdk = sdk.copy()
        return result
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }
}
@State(name = "TinyGoPluginUserConfig", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
internal class UserConfigurationImpl : PersistentStateComponent<UserConfigurationState> {
//    var userConfigurationState = UserConfigurationState()

    var userConfiguration = UserConfigurationAPI()

    override fun getState(): UserConfigurationState = userConfiguration.state

    override fun loadState(state: UserConfigurationState) {
        XmlSerializerUtil.copyBean(state, this.userConfiguration.state)
        this.userConfiguration.updateState()
    }
}
