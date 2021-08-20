package org.jetbrains.tinygoplugin.inspections

import com.goide.SdkAware
import com.goide.quickfix.GoQuickFixTestBase
import org.jetbrains.tinygoplugin.inspections.libraries.TinyGoMachineDependentTestInspection
import org.jetbrains.tinygoplugin.testFramework.setupTinyGo

@SdkAware("1.16")
class TinyGoMachineDependentTestInspectionTest : GoQuickFixTestBase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(TinyGoMachineDependentTestInspection())
        setupTinyGo(this, myFixture.project)
    }

    fun testNormalFileWithMachineImport() {
        doTest()
    }

    fun testTestWithMachineImport() {
        doQuickFixTest()
    }

    fun testTestWithOnlyMachineImport() {
        doQuickFixTest()
    }

    private fun doTest() {
        testHighlighting(getTestName(true) + ".go")
    }

    private fun doQuickFixTest() {
        val testName = getTestName(true)
        testHighlighting("${testName}_test.go")
        applySingleQuickFix("Remove usage of unsupported package")
        myFixture.checkResultByFile("${testName}_test-after.go")
    }

    override fun getTestDataPath(): String {
        return System.getProperty("user.dir") + "/src/test/testData/inspections/import-machine-tests"
    }
}
