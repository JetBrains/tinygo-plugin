package org.jetbrains.tinygoplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

@State(name = "TinyGo", storages = [Storage("tinygo_settings.xml")])
class TinyGoPluginConfiguration : PersistentStateComponent<TinyGoPluginConfiguration> {
    var tinyGoExecutablePath = File("")
    var targetPlatform = ""
    var gopath = File("")

    override fun getState(): TinyGoPluginConfiguration {
        return this
    }

    override fun loadState(state: TinyGoPluginConfiguration) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: TinyGoPluginConfiguration
            get() = ServiceManager.getService(TinyGoPluginConfiguration::class.java)
    }
}