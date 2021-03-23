package org.jetbrains.tinygoplugin.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.goide.sdk.combobox.GoBasedSdkChooserCombo
import com.goide.sdk.combobox.GoSdkActionsProvider
import com.goide.sdk.combobox.GoSdkListProvider
import com.goide.sdk.download.GoDownloadSdkAction
import com.goide.sdk.download.GoSdkDownloaderDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.HttpRequests
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.tinygoplugin.TinyGoBundle
import java.io.IOException
import java.util.function.Consumer

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

    //    override fun loadState(p0: TinyGoSdkList) {
//        XmlSerializerUtil.copyBean(state, this)
//        loadedSdks = savedSdks.map { TinyGoSdk(it.sdkUrl, it.version.toString()) }.toMutableList()
//    }
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

private const val TINYGO_SDK_TITLE = "tinygoSDK.download.title"
private const val TINYGO_SDK_PROGRESS_ICON_NAME = "tinygoSDK.download.progress"
private const val TINYGO_GITHUB = "tinygo-org/tinygo"

class TinyGoDownloaderDialog : GoSdkDownloaderDialog<TinyGoSdk> {
    override fun createDownloadingSdk(version: String, path: String): TinyGoSdk {
        if (path.isEmpty()) {
            return TinyGoDownloadingSdk(version, null)
        }
        val result = TinyGoDownloadingSdk(version, path)
        TinyGoDownloadSdkService.getInstance().downloadTinyGoSdk(result)
        return result
    }

    override fun getTitle(): String = TinyGoBundle.getMessage(TINYGO_SDK_TITLE)

    override fun getProgressIconName(): String = TinyGoBundle.getMessage(TINYGO_SDK_PROGRESS_ICON_NAME)

    override fun discoverVersions(
        progressIndicator: ProgressIndicator,
        versionConsumer: Consumer<MutableCollection<String>>,
    ): Boolean {
        val tinyGoReleasesUrl = "http://api.github.com/repos/$TINYGO_GITHUB/releases"
        val request = HttpRequests.request(tinyGoReleasesUrl)
        try {
            val response = request.readString(progressIndicator)
            val objectMapper = ObjectMapper()
            val releases = objectMapper.readTree(response)
            if (!releases.isArray) {
                return false
            }
            val versions = releases.asSequence().map { node ->
                node["tag_name"].textValue()
            }.distinct().map { it.substring(1) }.sorted().toMutableList()
            versionConsumer.accept(versions)
        } catch (e: IOException) {
            return false
        }
        return true
    }
}

const val TINYGO_LOCAL_FILE_DESCRIPTOR_TITLE = "tinygoSDK.local.fileDescriptor"

class TinyGoLocalSdkAction(private val combo: GoBasedSdkChooserCombo<TinyGoSdk>) : DumbAwareAction({ "Local..." }) {

    override fun actionPerformed(e: AnActionEvent) {
        val selectedDir = combo.sdk.root()
        val suggestedDirectory = suggestSdkDirectory()
        var preselection = selectedDir
        if (preselection == null && suggestedDirectory != null) {
            preselection = VfsUtil.findFile(suggestedDirectory.toPath(), false)
        }
        val descriptor = object : FileChooserDescriptor(false, true, false, false, false, false) {
            override fun validateSelectedFiles(files: Array<out VirtualFile>) {
                if (files.isNotEmpty()) {
                    val valid = checkDirectoryForTinyGo(files[0])
                    if (!valid) {
                        throw Exception("Selected directory is not a valid TinyGo SDK")
                    }
                }
            }
        }
        descriptor.title = TinyGoBundle.message(TINYGO_LOCAL_FILE_DESCRIPTOR_TITLE)
        FileChooser.chooseFile(
            descriptor, null, combo, preselection
        ) { selectedFile ->
            val sdk = TinyGoSdk(selectedFile.url, null)
            if (sdk.isValid) {
                if (e.project != null) {
                    sdk.computeVersion(e.project!!) {
                        combo.addSdk(sdk, true)
                        TinyGoSdkList.getInstance().addSdk(sdk)
                    }
                }
            } else {
                Messages.showErrorDialog(
                    combo,
                    "Error message",
                    TinyGoBundle.message(TINYGO_LOCAL_FILE_DESCRIPTOR_TITLE)
                )
            }
        }
    }
}

class TinyGoSdkChooserCombo :
    GoBasedSdkChooserCombo<TinyGoSdk>(
        object : GoSdkListProvider<TinyGoSdk> {
            override fun getAllKnownSdks(): MutableList<TinyGoSdk> {
                val loadedSdks = TinyGoSdkList.getInstance().loadedSdks.toList()
                val downloadingSdks = TinyGoDownloadSdkService.getInstance().downloadingTinyGoSdks.toList()
                return (loadedSdks + downloadingSdks).toMutableList()
            }

            override fun discoverSdks(state: ModalityState?): MutableList<TinyGoSdk> = allKnownSdks
        },
        GoSdkActionsProvider {
            listOf(
                TinyGoLocalSdkAction(it),
                GoDownloadSdkAction(
                    it,
                    TinyGoDownloaderDialog(),
                    VersionComparatorUtil.COMPARATOR.reversed()
                )
            )
        },
        nullSdk
    )
