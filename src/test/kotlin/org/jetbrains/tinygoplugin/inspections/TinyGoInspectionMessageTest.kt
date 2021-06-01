package org.jetbrains.tinygoplugin.inspections

import org.junit.Assert.assertEquals
import org.junit.Test

class TinyGoInspectionMessageTest {

    @Test
    fun testToString() {
        val message = "message"
        assertEquals(message, TinyGoInspectionMessage(message).toString())
    }
}
