package org.jetbrains.tinygoplugin.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private const val NO_ERROR_TINYGO_OUTPUT = """
        [GOROOT=/path/to/go/go1.16.7 #gosetup
        , GOPATH=/path/to/go #gosetup
        , tinygo info -target arduino -scheduler none -gc conservative #gosetup
        , TinyGo is a Go compiler for small places.
        , version: 0.16.0
        , usage: tinygo command [-printir] [-target=<target>] -o <output> <input>
    """

private fun prepareTestInput(current: String, min: String, max: String) = """
        [GOROOT=/path/to/go/go1.16.7 #gosetup
        , GOPATH=/path/to/go #gosetup
        , tinygo info -target arduino -scheduler none -gc conservative #gosetup
        , requires go version $min through $max, got go$current
        , TinyGo is a Go compiler for small places.
        , version: 0.16.0
        , usage: tinygo command [-printir] [-target=<target>] -o <output> <input>
    """

internal class TinyGoInfoExtractorKtTest {
    @Test fun testNoErrorProvided() {
        val computedVersionTriple = extractIncompatibleVersionsError(NO_ERROR_TINYGO_OUTPUT)
        assertNull(computedVersionTriple)
    }

    private fun doExtractionTest(current: String, min: String, max: String) {
        val wantedVersionsTriple = Triple(current, min, max)
        val testInput = prepareTestInput(current, min, max)
        val computedVersionTriple = extractIncompatibleVersionsError(testInput)
        assertNotNull(computedVersionTriple)
        assertEquals(computedVersionTriple, wantedVersionsTriple)
    }

    @Test fun testTwoDigitMinorVerGoVersionExtraction() {
        doExtractionTest("1.16", "1.11", "1.15")
    }

    @Test fun testOneDigitMinorVerGoVersionExtraction() {
        doExtractionTest("1.6", "1.8", "1.9")
    }
}
