package org.jetbrains.tinygoplugin.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient

data class TinyGoSdkSerialized(var sdkUrl: String = "", var version: TinyGoSdkVersion = unknownVersion) :
    Comparable<TinyGoSdkSerialized> {
    override fun compareTo(other: TinyGoSdkSerialized): Int {
        return VersionComparatorUtil.COMPARATOR.reversed().compare(version.toString(), other.version.toString())
    }
}

private fun MutableSet<TinyGoSdkSerialized>.add(sdkUrl: String, version: TinyGoSdkVersion) = this.add(
    TinyGoSdkSerialized(sdkUrl, version)
)

data class TinyGoSdkListState(
    var savedSdks: MutableSet<TinyGoSdkSerialized> = HashSet(),
)

@State(name = "TinyGoSdkList", storages = [Storage("tinygo.sdk.xml")])
@Service(Service.Level.APP)
class TinyGoSdkList : PersistentStateComponent<TinyGoSdkListState> {
    companion object {
        fun getInstance(): TinyGoSdkList = ApplicationManager.getApplication().getService(TinyGoSdkList::class.java)
    }

    private var state = TinyGoSdkListState()

    @Transient
    @JvmField
    var loadedSdks: MutableList<TinyGoSdk> = ArrayList()

    override fun getState(): TinyGoSdkListState {
        saveLoadedSdk()
        return state
    }

    override fun loadState(state: TinyGoSdkListState) {
        XmlSerializerUtil.copyBean(state, this.state)
        loadedSdks = state.savedSdks.map { TinyGoSdk(it.sdkUrl, it.version.toString()) }.toMutableList()
    }

    fun addSdk(sdk: TinyGoSdk) {
        synchronized(state) {
            val added = state.savedSdks.add(sdk.homeUrl, sdk.sdkVersion)
            if (added) {
                loadedSdks.add(sdk)
            }
        }
    }

    private fun saveLoadedSdk() {
        synchronized(state) {
            loadedSdks.forEach { state.savedSdks.add(it.homeUrl, it.sdkVersion) }
        }
    }
}
