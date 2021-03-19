package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionMessage
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.tinygoplugin.BUNDLE
import org.jetbrains.tinygoplugin.TinyGoBundle

class TinyGoInspectionMessage(private val message: String) : GoInspectionMessage {
    override fun toString(): String = message
}

fun inspectionMessage(@PropertyKey(resourceBundle = BUNDLE) key: String): GoInspectionMessage {
    val message = TinyGoBundle.message(key)
    return TinyGoInspectionMessage(message)
}

fun inspectionMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg args: Any): GoInspectionMessage {
    val message = TinyGoBundle.message(key, args)
    return TinyGoInspectionMessage(message)
}
