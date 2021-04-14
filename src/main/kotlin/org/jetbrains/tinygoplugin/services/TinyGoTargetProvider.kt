package org.jetbrains.tinygoplugin.services

import com.intellij.json.JsonFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.nullSdk

fun tinygoTargets(sdk: TinyGoSdk): MutableList<String> {
    if (sdk == nullSdk) {
        return mutableListOf()
    }
    val targetsFolder = sdk.sdkRoot?.findChild("targets") ?: return mutableListOf()
    return targetsFolder.children.filter { it.fileType == JsonFileType.INSTANCE }
        .map(VirtualFile::getNameWithoutExtension).toMutableList()
}
