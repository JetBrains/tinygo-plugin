package org.jetbrains.tinygoplugin.services

import com.goide.GoCodeInsightFixtureTestCase
import com.intellij.openapi.components.service
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.sdk.TinyGoDownloadSdkService
import org.jetbrains.tinygoplugin.sdk.TinyGoDownloadingSdk

internal class TinyGoConfigurationWithTagUpdateTest : GoCodeInsightFixtureTestCase() {
    fun testDownloadedSdkReplacesDownloadingSdk() {
        var callbacks = 0
        val settings = TinyGoConfigurationWithTagUpdate(TinyGoConfiguration.getInstance(), project) { callbacks++ }
        val downloadingSdk = TinyGoDownloadingSdk("0.44.0", "/tmp/${getTestName(true)}")
        settings.sdk = downloadingSdk

        service<TinyGoDownloadSdkService>().completeDownload(downloadingSdk, downloadingSdk.toLocalTinyGoSdk())
        PlatformTestUtil.waitWithEventsDispatching(
            "The downloading SDK stays selected",
            { settings.sdk !is TinyGoDownloadingSdk },
            10,
        )

        assertEquals(downloadingSdk.homeUrl, settings.sdk.homeUrl)
        assertEquals(2, callbacks)
    }

    fun testSelectionOfDownloadedSdkGivesLocalSdk() {
        val downloadingSdk = TinyGoDownloadingSdk("0.44.0", "/tmp/${getTestName(true)}")
        service<TinyGoDownloadSdkService>().completeDownload(downloadingSdk, downloadingSdk.toLocalTinyGoSdk())
        val settings = TinyGoConfigurationWithTagUpdate(TinyGoConfiguration.getInstance(), project) { }

        settings.sdk = downloadingSdk

        assertFalse(settings.sdk is TinyGoDownloadingSdk)
        assertEquals(downloadingSdk.homeUrl, settings.sdk.homeUrl)
    }

    override fun getTestDataPath(): String = System.getProperty("user.dir") + "/src/test/testData"
}
