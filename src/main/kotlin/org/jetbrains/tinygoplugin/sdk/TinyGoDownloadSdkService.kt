package org.jetbrains.tinygoplugin.sdk

import com.goide.GoNotifications
import com.goide.GoOsManager
import com.goide.sdk.GoSdk
import com.goide.util.GoUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.platform.templates.github.DownloadUtil
import com.intellij.platform.templates.github.ZipUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.util.PathUtil
import com.intellij.util.io.Decompressor
import org.jetbrains.tinygoplugin.configuration.TinyGoSdkList
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.function.Consumer

private const val TINYGO_GITHUB = "tinygo-org/tinygo"

@Service
@Suppress("NestedBlockDepth")
class TinyGoDownloadSdkService private constructor() {
    companion object {
        val logger: Logger = Logger.getInstance(TinyGoDownloadSdkService::class.java)

        fun getInstance(): TinyGoDownloadSdkService =
            ApplicationManager.getApplication().getService(TinyGoDownloadSdkService::class.java)
    }

    val downloadingTinyGoSdks: MutableSet<TinyGoDownloadingSdk> = mutableSetOf()

    fun downloadTinyGoSdk(sdk: TinyGoDownloadingSdk, onFinish: Consumer<TinyGoSdk>) {
        if (sdk.isDownloaded) {
            return
        }
        val registered = synchronized(downloadingTinyGoSdks) {
            downloadingTinyGoSdks.add(sdk)
        }
        if (registered) {
            startDownloading(sdk, onFinish)
        }
    }

    private fun startDownloading(sdk: TinyGoDownloadingSdk, onFinish: Consumer<TinyGoSdk>) {

        val downloadTask: Task.Backgroundable = object : Task.Backgroundable(null, "Downloading TinyGo SDK", true) {
            override fun onFinished() {
                synchronized(downloadingTinyGoSdks) {
                    downloadingTinyGoSdks.remove(sdk)
                }
                val localSdk = sdk.toLocalTinyGoSdk()
                if (localSdk == GoSdk::NULL) {
                    return
                }
                TinyGoSdkList.getInstance().addSdk(localSdk)
                sdk.isDownloaded = true
                GoNotifications.getGeneralGroup()
                    .createNotification("Downloaded SDK", NotificationType.INFORMATION)
                    .notify(null)
                onFinish.accept(localSdk)
            }

            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.isIndeterminate = false
                    val extension = if (GoOsManager.isWindows()) ".zip" else ".tar.gz"
                    val fileName = "tinygo${sdk.version}.${GoUtil.systemOS()}-${GoUtil.systemArch()}$extension"
                    val downloadedArchive = Files.createTempFile("for-actual-downloading-", extension)
                    DownloadUtil.downloadContentToFile(
                        indicator,
                        "https://github.com/$TINYGO_GITHUB/releases/download/v${sdk.version}/$fileName",
                        downloadedArchive.toFile()
                    )
                    indicator.text2 = ""
                    // checksum verifying??
                    unpackSdk(indicator, downloadedArchive, VfsUtilCore.urlToPath(sdk.homeUrl))
                } catch (e: IOException) {
                    logger.error(e.message)
                }
            }

            private fun unpackSdk(indicator: ProgressIndicator, archive: Path, targetPath: String) {
                indicator.isIndeterminate = false
                indicator.text = "Unpacking"
                try {
                    val tempDirectory = Files.createTempDirectory("downloaded-sdk-")
                    if (PathUtil.getFileExtension(archive.fileName.toString()).equals("zip")) {
                        ZipUtil.unzip(
                            indicator, tempDirectory.toFile(),
                            archive.toFile(), null,
                            null, false
                        )
                    } else {
                        Decompressor.Tar(archive).extract(tempDirectory)
                    }
                    val tinyGo = tempDirectory.resolve("tinygo")
                    if (!Files.exists(tinyGo) || !Files.isDirectory(tinyGo)) {
                        error("Could not find tinygo directory in downloaded directory")
                    }
                    val targetDir = NioFiles.createDirectories(Paths.get(targetPath))
                    copyDir(tinyGo, targetDir, indicator)
                    LocalFileSystem.getInstance().refreshNioFiles(Collections.singleton(targetDir))
                    FileUtil.asyncDelete(tinyGo.toFile())
                } catch (e: IOException) {
                    error("Error unpacking TinyGoSDK", e, null, onFinish)
                }
            }

            private fun copyDir(from: Path, to: Path, indicator: ProgressIndicator) {
                try {
                    Files.walk(from).use { filesStream ->
                        filesStream.forEach { f ->
                            try {
                                indicator.checkCanceled()
                                val targetPath = to.resolve(from.relativize(f))
                                Files.copy(
                                    f,
                                    targetPath,
                                    StandardCopyOption.REPLACE_EXISTING,
                                    StandardCopyOption.COPY_ATTRIBUTES
                                )
                            } catch (e: IOException) {
                                throw UncheckedIOException(e.message, e)
                            }
                        }
                    }
                } catch (e: UncheckedIOException) {
                    error("Error unpacking TinyGoSdk", e, null, onFinish)
                }
            }

            private fun error(message: String, e: Exception?, details: String?, onFinished: Consumer<TinyGoSdk>) {
                GoNotifications.getGeneralGroup().createNotification(
                    "Failed to download TinyGo SDK",
                    message,
                    NotificationType.ERROR
                ).addAction(object : NotificationAction(RefactoringBundle.message("retry.command")) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        notification.expire()
                        if (!sdk.isDownloaded) {
                            this@TinyGoDownloadSdkService.downloadTinyGoSdk(sdk, onFinished)
                        }
                    }
                }).notify(null)
                logger.info(message + ". " + StringUtil.notNullize(details), e)
            }
        }

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            downloadTask, BackgroundableProcessIndicator(downloadTask)
        )
    }
}
