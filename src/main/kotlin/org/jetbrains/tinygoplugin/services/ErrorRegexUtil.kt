package org.jetbrains.tinygoplugin.services

import com.goide.sdk.GoSdkService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.tinygoplugin.TinyGoBundle

private const val TINYGO_INCOMPATIBLE_GO_VERSION_UNKNOWN = "tinygoSDK.incompatibleGoVersion.unknownVersion"
private const val TINYGO_INCOMPATIBLE_GO_VERSION_TITLE = "tinygoSDK.incompatibleGoVersion.title"
private const val TINYGO_INCOMPATIBLE_GO_VERSION_RANGE_SPECIFIED_MESSAGE =
    "tinygoSDK.incompatibleGoVersion.rangeSpecified.message"
private const val TINYGO_INCOMPATIBLE_GO_VERSION_RANGE_UNSPECIFIED_MESSAGE =
    "tinygoSDK.incompatibleGoVersion.rangeUnspecified.message"

fun generateMessageIfVersionErrorFound(project: Project, processOutput: String): String? {
    val incompatibleGoVersionsMatch = extractIncompatibleVersionsError(processOutput)
    val errorMessage =
        when {
            incompatibleGoVersionsMatch != null -> {
                val (currentVer, oldestVer, latestVer) = incompatibleGoVersionsMatch
                TinyGoBundle.message(
                    TINYGO_INCOMPATIBLE_GO_VERSION_RANGE_SPECIFIED_MESSAGE,
                    currentVer,
                    oldestVer,
                    latestVer
                )
            }
            extractCouldNotReadGoVersionError(processOutput) -> {
                val goSdk = project.service<GoSdkService>().getSdk(null)
                val currentVer = goSdk.version ?: TinyGoBundle.message(TINYGO_INCOMPATIBLE_GO_VERSION_UNKNOWN)
                TinyGoBundle.message(
                    TINYGO_INCOMPATIBLE_GO_VERSION_RANGE_UNSPECIFIED_MESSAGE,
                    currentVer
                )
            }
            else -> null
        }

    if (errorMessage != null) {
        Messages.showErrorDialog(
            project,
            errorMessage,
            TinyGoBundle.message(TINYGO_INCOMPATIBLE_GO_VERSION_TITLE)
        )
    }

    return errorMessage
}

@Suppress("MagicNumber")
fun extractIncompatibleVersionsError(msg: String): Triple<String, String, String>? {
    val minor2DigitsRule = "([0-9]\\.[0-9][0-9])"
    val minor1DigitRule = "([0-9]\\.[0-9])" // rule produces 3 groups: (1d or 2d), (1d), (2d)
    val goVersionRule = "($minor1DigitRule|$minor2DigitsRule)"
    val errorPattern =
        Regex("""requires go version $goVersionRule through $goVersionRule, got go$goVersionRule\n""")

    val errorMatch = errorPattern.find(msg) ?: return null
    val oldestCompatibleGoVersion = errorMatch.groupValues[1] // 1st = 2nd or 3rd
    val latestCompatibleGoVersion = errorMatch.groupValues[4] // 4th = 5th or 6th
    val currentGoVersion = errorMatch.groupValues[7] // 7th = 8th or 9th

    return Triple(currentGoVersion, oldestCompatibleGoVersion, latestCompatibleGoVersion)
}

fun extractCouldNotReadGoVersionError(msg: String): Boolean {
    val errorPattern =
        Regex("""could not read version from GOROOT \(((.*)/)*(.*)\): Invalid go version output:\n""")

    return errorPattern.containsMatchIn(msg)
}
