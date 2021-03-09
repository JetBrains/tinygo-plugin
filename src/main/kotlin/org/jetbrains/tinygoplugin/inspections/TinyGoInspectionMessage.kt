package org.jetbrains.tinygoplugin.inspections

import com.goide.inspections.core.GoInspectionMessage

class TinyGoInspectionMessage(private val message: String) : GoInspectionMessage {
    override fun getTemplate(): String = ""

    override fun toString(): String = message
}
