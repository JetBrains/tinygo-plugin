package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.nullSdk
import org.jetbrains.tinygoplugin.sdk.unknownVersion

interface UserConfiguration {
    var sdk: TinyGoSdk
}

internal interface UserConfigurationStorage {
    var sdkStorage: TinyGoSdkStorage
}

internal data class UserConfigurationStorageImpl(
    override var sdkStorage: TinyGoSdkStorage = TinyGoSdkStorage(
        "",
        unknownVersion
    ),
) :
    UserConfigurationStorage

internal fun TinyGoSdk.toStorage(): TinyGoSdkStorage {
    return TinyGoSdkStorage(this.homeUrl, this.sdkVersion)
}

internal fun TinyGoSdkStorage.toImpl(): TinyGoSdk {
    val homeUrl = if (sdkUrl.isEmpty()) null else sdkUrl
    return TinyGoSdk(homeUrl, this.version)
}

internal class UserConfigurationStorageWrapper : UserConfigurationStorage, UserConfiguration {
    internal fun updateState() {
        tinyGoSdk = state.sdkStorage.toImpl()
    }

    private var tinyGoSdk: TinyGoSdk = nullSdk
    var state: UserConfigurationStorageImpl = UserConfigurationStorageImpl()

    override var sdk: TinyGoSdk
        get() = tinyGoSdk
        set(value) {
            tinyGoSdk = value
            state.sdkStorage = value.toStorage()
        }
    override var sdkStorage: TinyGoSdkStorage
        get() = state.sdkStorage
        set(value) {
            state.sdkStorage = value
            tinyGoSdk = state.sdkStorage.toImpl()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserConfigurationStorageWrapper) return false
        return state == other.state
    }

    fun copy(): UserConfigurationStorageWrapper {
        val result = UserConfigurationStorageWrapper()
        result.sdkStorage = sdkStorage.copy()
        return result
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }
}
@State(name = "TinyGoPluginUserConfig", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
@Service(Service.Level.PROJECT)
internal class UserConfigurationImpl : PersistentStateComponent<UserConfigurationStorageImpl> {
    var myState = UserConfigurationStorageWrapper()

    override fun getState(): UserConfigurationStorageImpl = myState.state

    override fun loadState(state: UserConfigurationStorageImpl) {
        XmlSerializerUtil.copyBean(state, this.myState.state)
        this.myState.updateState()
    }
}
