package org.jetbrains.tinygoplugin.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

internal class TinyGoLibraryLayoutServiceTest {
    @Test
    fun testParseConcatenatedTinyGoListOutput() {
        val packages =
            parseTinyGoList(
                """
                {
                  "Dir": "/goroot/src/runtime/volatile",
                  "ImportPath": "runtime/volatile",
                  "GoFiles": ["volatile.go"]
                }
                {
                  "Dir": "/goroot/src/device/avr",
                  "ImportPath": "device/avr",
                  "GoFiles": ["atmega328p.go", "avr.go"],
                  "IgnoredGoFiles": ["atmega1280.go", "atmega2560.go"]
                }
                """.trimIndent(),
            )

        assertEquals(2, packages.size)
        assertEquals("device/avr", packages[1].importPath)
        assertEquals(listOf("atmega328p.go", "avr.go"), packages[1].goFiles)
        assertEquals(listOf("atmega1280.go", "atmega2560.go"), packages[1].ignoredGoFiles)
    }

    @Test
    fun testCalculateDeviceLayoutUsesToolchainSelection() {
        val deviceRoot = Path.of("/goroot/src/device")
        val packages =
            listOf(
                TinyGoListPackage(
                    directory = "/different-generated-goroot/src/device/avr",
                    importPath = "device/avr",
                    goFiles = listOf("atmega328p.go", "avr.go"),
                    ignoredGoFiles = listOf("atmega1280.go", "atmega2560.go"),
                ),
                TinyGoListPackage(
                    directory = "/goroot/src/runtime/volatile",
                    importPath = "runtime/volatile",
                    goFiles = listOf("volatile.go"),
                    ignoredGoFiles = emptyList(),
                ),
            )

        val layout = calculateDeviceLayout(deviceRoot, packages)

        assertEquals(setOf(deviceRoot.resolve("avr")), layout.selectedDirectories)
        assertEquals(
            setOf(
                deviceRoot.resolve("avr/atmega1280.go"),
                deviceRoot.resolve("avr/atmega2560.go"),
            ),
            layout.ignoredFiles,
        )
        assertTrue(deviceRoot.resolve("avr/atmega328p.go") !in layout.ignoredFiles)
    }
}
