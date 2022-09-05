@file:Suppress("NoMultipleSpaces", "Unnecessary space(s)")
package org.jetbrains.tinygoplugin.configuration

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.intellij.json.JsonFileType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import java.io.File
import java.util.stream.Collectors
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/* ktlint-disable */
data class TinyGoTarget constructor(
    @SerializedName("inherits")             var inherits: MutableSet<String>? = mutableSetOf(),
    @SerializedName("llvm-target")          var triple: String?,
    @SerializedName("cpu")                  var cpu: String?,
    @SerializedName("features")             var features: String?,
    @SerializedName("goos")                 var goOS: String?,
    @SerializedName("goarch")               var goArch: String?,
    @SerializedName("build-tags")           var buildTags: MutableSet<String>? = mutableSetOf(),
    @SerializedName("gc")                   var gc: String?,
    @SerializedName("scheduler")            var scheduler: String?,
    @SerializedName("serial")               var serial: String?,
    @SerializedName("linker")               var linker: String?,
    @SerializedName("rtlib")                var rtLib: String?,
    @SerializedName("libc")                 var libc: String?,
    @SerializedName("automatic-stack-size") var autoStackSize: Boolean,
    @SerializedName("default-stack-size")   var defaultStackSize: Int,
    @SerializedName("cflags")               var cflags: MutableSet<String>? = mutableSetOf(),
    @SerializedName("ldflags")              var ldflags: MutableSet<String>? = mutableSetOf(),
    @SerializedName("linkerscript")         var linkerScript: String?,
    @SerializedName("extra-files")          var extraFiles: MutableSet<String>? = mutableSetOf(),
    @SerializedName("rp2040-boot-patch")    var rp2040BootPatch: Boolean?,
    // inherited Emulator must not be appended
    @SerializedName("emulator")             var emulator: String?,
    @SerializedName("flash-command")        var flashCommand: String?,
    @SerializedName("gdb")                  var gdb: MutableSet<String>? = mutableSetOf(),
    @SerializedName("flash-1200-bps-reset") var portReset: String?,
    @SerializedName("serial-port")          var serialPort: MutableSet<String>? = mutableSetOf(),
    @SerializedName("flash-method")         var flashMethod: String?,
    @SerializedName("msd-volume-name")      var flashVolume: String?,
    @SerializedName("msd-firmware-name")    var flashFilename: String?,
    @SerializedName("uf2-family-id")        var uf2FamilyID: String?,
    @SerializedName("binary-format")        var binaryFormat: String?,
    @SerializedName("openocd-interface")    var openOCDInterface: String?,
    @SerializedName("openocd-target")       var openOCDTarget: String?,
    @SerializedName("openocd-transport")    var openOCDTransport: String?,
    @SerializedName("openocd-commands")     var openOCDCommands: MutableSet<String>? = mutableSetOf(),
    @SerializedName("jlink-device")         var jLinkDevice: String?,
    @SerializedName("code-model")           var codeModel: String?,
    @SerializedName("relocation-model")     var relocationModel: String?,
    @SerializedName("wasm-abi")             var wasmAbi: String?
)
/* ktlint-disable */

private fun supplyJsonPath(src: String, sdkRoot: VirtualFile): String {
    val f = File(src)
    return if (f.isFile && f.extension == JsonFileType.DEFAULT_EXTENSION) {
        f.path
    } else {
        sdkRoot.findChild("targets")?.findChild("$src.json")?.path ?: ""
    }
}

fun createTargetWrapper(wrapper: TinyGoPropertiesWrapper): TinyGoTarget? {
    val sdkRoot = wrapper.tinyGoSdkPath.get().sdkRoot ?: return null
    val targetObj = readTargetJson(wrapper.target.get(), sdkRoot) ?: return null
    targetObj.performInheritance(sdkRoot)
    targetObj.applyTinyGoFlags(wrapper.obj.tinyGoSettings)
    targetObj.inherits?.clear()
    return targetObj
}

fun TinyGoTarget.serialize(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(this)
}

fun readTargetJson(pathToTarget: String, sdkRoot: VirtualFile): TinyGoTarget? {
    val jsonFile = File(supplyJsonPath(pathToTarget, sdkRoot))
    if (!jsonFile.exists()) return null

    val gsonBuilder = Gson()
    return try {
        gsonBuilder.fromJson(jsonFile.readText(), TinyGoTarget::class.java)
    } catch (e: JsonSyntaxException) {
        logger<TinyGoTarget>().error(e)
        null
    }
}

fun TinyGoTarget.performInheritance(sdkRoot: VirtualFile) {
    val gsonBuilder = Gson()

    val inherits = inherits?.stream()
        ?.map { parent -> File(supplyJsonPath(parent, sdkRoot)) }
        ?.filter { it.exists() }
        ?.map { gsonBuilder.fromJson(it.readText(), TinyGoTarget::class.java) }
        ?.collect(Collectors.toSet()) ?: return

    for (parent in inherits) {
        if (parent.inherits != null) {
            parent.performInheritance(sdkRoot)
        }
        for (flag in TinyGoTarget::class.memberProperties) {
            val myVal = flag.get(this)
            val parentVal = flag.get(parent)
            if (parentVal == null || parentVal == myVal || flag !is KMutableProperty<*>) continue
            if (myVal is MutableSet<*> && parentVal is MutableSet<*>) {
                myVal.toMutableSet().addAll(parentVal.filterIsInstance<String>())
            } else if (myVal == null || myVal is Int) {
                flag.setter.call(this, parentVal)
            }
        }
    }
}

fun TinyGoTarget.applyTinyGoFlags(settings: TinyGoConfiguration) {
    this.gc = settings.gc.cmd
    this.scheduler = settings.scheduler.cmd
    this.goArch = settings.goArch
    this.goOS = settings.goOS
    this.buildTags = settings.goTags
        .split(" ")
        .filter {
            !it.startsWith("gc.") && !it.startsWith("scheduler.") &&
                !it.startsWith("serial.") && it != "tinygo"
        }
        .toMutableSet()
}
