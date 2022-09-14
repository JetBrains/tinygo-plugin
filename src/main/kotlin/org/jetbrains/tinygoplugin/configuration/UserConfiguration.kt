package org.jetbrains.tinygoplugin.configuration

import com.goide.sdk.GoSdk
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
    var cachedGoRoot: GoSdk
}

data class CachedGoRootStorage(var sdkUrl: String = "")

internal interface UserConfigurationStorage {
    var sdkStorage: TinyGoSdkStorage
    var cachedGoRootStorage: CachedGoRootStorage
}

internal data class UserConfigurationStorageImpl(
    override var sdkStorage: TinyGoSdkStorage = TinyGoSdkStorage(
        "",
        unknownVersion
    ),
    override var cachedGoRootStorage: CachedGoRootStorage = CachedGoRootStorage()
) :
    UserConfigurationStorage

internal fun TinyGoSdk.toStorage(): TinyGoSdkStorage {
    return TinyGoSdkStorage(this.homeUrl, this.sdkVersion)
}

internal fun TinyGoSdkStorage.toImpl(): TinyGoSdk {
    val homeUrl = sdkUrl.ifEmpty { null }
    return TinyGoSdk(homeUrl, this.version)
}

internal fun GoSdk.toStorage(): CachedGoRootStorage {
    return CachedGoRootStorage(this.homeUrl)
}

internal fun CachedGoRootStorage.toImpl(): GoSdk {
    val homeUrl = sdkUrl.ifEmpty { null }
    return GoSdk.fromUrl(homeUrl)
}

internal class UserConfigurationStorageWrapper : UserConfigurationStorage, UserConfiguration {
    internal fun updateState() {
        tinyGoSdk = state.sdkStorage.toImpl()
        cachedGoRoot = state.cachedGoRootStorage.toImpl()
    }

    private var tinyGoSdk: TinyGoSdk = nullSdk
    private var tinyGoCachedGoRoot: GoSdk = GoSdk.NULL
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

    override var cachedGoRoot: GoSdk
        get() = tinyGoCachedGoRoot
        set(value) {
            tinyGoCachedGoRoot = value
            state.cachedGoRootStorage = value.toStorage()
        }
    override var cachedGoRootStorage: CachedGoRootStorage
        get() = state.cachedGoRootStorage
        set(value) {
            state.cachedGoRootStorage = value
            tinyGoCachedGoRoot = state.cachedGoRootStorage.toImpl()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserConfigurationStorageWrapper) return false
        return state == other.state
    }

    fun copy(): UserConfigurationStorageWrapper {
        val result = UserConfigurationStorageWrapper()
        result.sdkStorage = sdkStorage.copy()
        result.cachedGoRootStorage = cachedGoRootStorage.copy()
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
