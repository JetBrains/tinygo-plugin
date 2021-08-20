package org.jetbrains.tinygoplugin.inspections

import com.goide.SdkAware
import com.goide.quickfix.GoQuickFixTestBase
import org.jetbrains.tinygoplugin.inspections.libraries.TinyGoImportInspection
import org.jetbrains.tinygoplugin.testFramework.setupTinyGo

@SdkAware("1.16")
class TinyGoImportInspectionTest : GoQuickFixTestBase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(TinyGoImportInspection())
        setupTinyGo(this, myFixture.project)
    }

    fun testNetHttpImport() {
        doQuickFixTest()
    }

    fun testOnlyNetHttpImport() {
        doQuickFixTest()
    }

    private fun doQuickFixTest() {
        val testName = getTestName(true)
        testHighlighting("$testName.go")
        applySingleQuickFix("Remove usage of unsupported package")
        myFixture.checkResultByFile("$testName-after.go")
    }

    override fun getTestDataPath(): String {
        return System.getProperty("user.dir") + "/src/test/testData/inspections/import-unsupported"
    }
}
