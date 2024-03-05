package org.jetbrains.tinygoplugin.icon

import com.intellij.openapi.util.IconLoader

object TinyGoPluginIcons {

    @JvmField
    val TinyGoIcon = IconLoader.getIcon("/icons/tinyGo.svg", javaClass)

    @JvmField
    val TinyGoLibraryIcon = TinyGoIcon

    @JvmField
    val TinyGoAsmIcon = IconLoader.getIcon("/icons/fileTypes/asm.svg", javaClass)
}
