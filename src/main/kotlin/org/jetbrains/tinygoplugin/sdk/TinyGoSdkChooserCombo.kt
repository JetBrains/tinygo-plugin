package org.jetbrains.tinygoplugin.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.goide.sdk.combobox.GoBasedSdkChooserCombo
import com.goide.sdk.combobox.GoSdkActionsProvider
import com.goide.sdk.combobox.GoSdkListProvider
import com.goide.sdk.download.GoDownloadSdkAction
import com.goide.sdk.download.GoSdkDownloaderDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.HttpRequests
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.configuration.TinyGoSdkList
import java.io.IOException
import java.util.function.Consumer

private const val TINYGO_SDK_TITLE = "tinygoSDK.download.title"
private const val TINYGO_SDK_PROGRESS_ICON_NAME = "tinygoSDK.download.progress"
const val TINYGO_GITHUB = "tinygo-org/tinygo"

class TinyGoDownloaderDialog(private val onFinish: Consumer<TinyGoSdk>) : GoSdkDownloaderDialog<TinyGoSdk> {
    companion object {
        val logger = logger<TinyGoDownloaderDialog>()
    }

    override fun createDownloadingSdk(version: String, path: String): TinyGoSdk {
        if (path.isEmpty()) {
            return TinyGoDownloadingSdk(version, null)
        }
        val result = TinyGoDownloadingSdk(version, path)
        logger.debug("Download of TinyGo SDK requested")
        TinyGoDownloadSdkService.getInstance().downloadTinyGoSdk(result, onFinish)
        return result
    }

    override fun getTitle(): String = TinyGoBundle.getMessage(TINYGO_SDK_TITLE)

    override fun getProgressIconName(): String = TinyGoBundle.getMessage(TINYGO_SDK_PROGRESS_ICON_NAME)

    override fun discoverVersions(
        os: String,
        arch: String,
        progressIndicator: ProgressIndicator,
        versionConsumer: Consumer<MutableCollection<String>>,
    ): Boolean {
        val tinyGoReleasesUrl = "https://api.github.com/repos/$TINYGO_GITHUB/releases"
        val request = HttpRequests.request(tinyGoReleasesUrl)
        @Suppress("SwallowedException")
        try {
            val response = request.readString(progressIndicator)
            val objectMapper = ObjectMapper()
            val releases = objectMapper.readTree(response)
            if (!releases.isArray) {
                return false
            }
            val versions = releases.asSequence().filter { node ->
                node["assets"].asSequence().filter { asset ->
                    val emulatedArch = osManager.emulatedArch(arch)
                    asset["name"].textValue().matches(Regex(".*[.]($os-$emulatedArch)[.](tar.gz|zip|deb)"))
                }.any()
            }.map { node ->
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
const val TINYGO_LOCAL_ERROR_INVALID_DIR = "tinygoSDK.local.error"

class TinyGoLocalSdkAction(private val combo: GoBasedSdkChooserCombo<TinyGoSdk>) : DumbAwareAction({ "Local..." }) {
    companion object {
        val logger = logger<TinyGoLocalSdkAction>()
    }

    override fun actionPerformed(e: AnActionEvent) {
        logger.debug("Select local SDK action triggered")

        val selectedDir = combo.sdk.sdkRoot
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
                        throw IllegalArgumentException(TinyGoBundle.message(TINYGO_LOCAL_ERROR_INVALID_DIR))
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
                val project = e.project ?: DefaultProjectFactory.getInstance().defaultProject
                sdk.computeVersion(project) {
                    combo.addSdk(sdk, true)
                    TinyGoSdkList.getInstance().addSdk(sdk)
                }
                logger.debug("Selected local TinyGo SDK is valid")
            } else {
                Messages.showErrorDialog(
                    combo,
                    TinyGoBundle.message(TINYGO_LOCAL_ERROR_INVALID_DIR),
                    TinyGoBundle.message(TINYGO_LOCAL_FILE_DESCRIPTOR_TITLE)
                )
                logger.debug("Selected local TinyGo SDK is invalid")
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
                    { null },
                    it,
                    TinyGoDownloaderDialog { sdk -> it.addSdk(sdk, true) },
                    VersionComparatorUtil.COMPARATOR.reversed()
                )
            )
        },
        nullSdk
    )
