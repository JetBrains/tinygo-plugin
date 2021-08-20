package org.jetbrains.tinygoplugin.inspections

import com.goide.GoCodeInsightFixtureTestCase
import org.jetbrains.tinygoplugin.testFramework.setupTinyGo

class TinyGoInterfaceInspectionTest : GoCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(TinyGoInterfaceInspection())
        setupTinyGo(this, myFixture.project)
    }

    fun testInterfaceEqComparison() {
        doTest()
    }

    fun testInterfaceNeqComparison() {
        doTest()
    }

    fun testNamedInterfaceComparison() {
        doTest()
    }

    fun testNamedInterfaceReferenceComparison() {
        doTest()
    }

    private fun doTest() {
        val testName = getTestName(true)
        testHighlighting("$testName.go")
    }

    override fun getTestDataPath(): String {
        return System.getProperty("user.dir") + "/src/test/testData/inspections/interface-comparison"
    }
}
