package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.TinyGoSdkVersion
import org.jetbrains.tinygoplugin.sdk.unknownVersion

data class TinyGoSdkStorage(var sdkUrl: String = "", var version: TinyGoSdkVersion = unknownVersion) :
    Comparable<TinyGoSdkStorage> {
    override fun compareTo(other: TinyGoSdkStorage): Int {
        return VersionComparatorUtil.COMPARATOR.reversed().compare(version.toString(), other.version.toString())
    }
}

private fun MutableSet<TinyGoSdkStorage>.add(sdkUrl: String, version: TinyGoSdkVersion) = this.add(
    TinyGoSdkStorage(sdkUrl, version)
)

data class TinyGoSdkListStorage(
    var savedSdks: MutableSet<TinyGoSdkStorage> = HashSet(),
)

@State(name = "TinyGoSdkList", storages = [Storage("tinygo.sdk.xml")])
@Service(Service.Level.APP)
class TinyGoSdkList : PersistentStateComponent<TinyGoSdkListStorage> {
    companion object {
        fun getInstance(): TinyGoSdkList = ApplicationManager.getApplication().getService(TinyGoSdkList::class.java)
    }

    private var state = TinyGoSdkListStorage()

    @Transient
    @JvmField
    var loadedSdks: MutableList<TinyGoSdk> = ArrayList()

    override fun getState(): TinyGoSdkListStorage {
        saveLoadedSdk()
        return state
    }

    override fun loadState(state: TinyGoSdkListStorage) {
        XmlSerializerUtil.copyBean(state, this.state)
        loadedSdks = state.savedSdks.map { TinyGoSdk(it.sdkUrl, it.version.toString()) }.toMutableList()
    }

    fun addSdk(sdk: TinyGoSdk) =
        lockStorage {
            val added = state.savedSdks.add(sdk.homeUrl, sdk.sdkVersion)
            if (added) {
                loadedSdks.add(sdk)
            }
        }

    private fun saveLoadedSdk() = lockStorage {
        loadedSdks.forEach { state.savedSdks.add(it.homeUrl, it.sdkVersion) }
    }
}

internal inline fun TinyGoSdkList.lockStorage(block: () -> Unit): Unit =
    synchronized(state, block)
