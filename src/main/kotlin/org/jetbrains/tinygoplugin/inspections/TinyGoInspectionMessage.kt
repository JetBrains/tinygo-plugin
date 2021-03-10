package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionMessage
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.tinygoplugin.BUNDLE
import org.jetbrains.tinygoplugin.TinyGoBundle

class TinyGoInspectionMessage(private val message: String) : GoInspectionMessage {
    override fun getTemplate(): String = ""

    override fun toString(): String = message
}

fun inspectionMessage(@PropertyKey(resourceBundle = BUNDLE) key: String): GoInspectionMessage {
    val message = TinyGoBundle.message(key)
    return TinyGoInspectionMessage(message)
}
