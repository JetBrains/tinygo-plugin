package org.jetbrains.tinygoplugin.sdk

import org.junit.Test
import kotlin.test.assertEquals

class TinyGoSdkUtilTest {
    @Test fun testParseTinyGoVersion() {
        assertEquals(TinyGoSdkVersion(0, 19, 0), tinyGoSdkVersion("0.19.0"))
        assertEquals(unknownVersion, tinyGoSdkVersion("0190"))
        assertEquals(unknownVersion, tinyGoSdkVersion("0.19"))
        assertEquals(unknownVersion, tinyGoSdkVersion("0.19.a"))
    }

    @Test fun testVersionArithmetic() {
        assert(TinyGoSdkVersion(0, 19, 0).isAtLeast(TinyGoSdkVersion(0, 15, 0)))
        assert(TinyGoSdkVersion(0, 16, 0).isLessThan(TinyGoSdkVersion(0, 18, 0)))
    }
}
