package org.jetbrains.tinygoplugin.inspections

import com.goide.GoCodeInsightFixtureTestCase
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.testFramework.setupTinyGo

internal class TinyGoStatementInspectionTest : GoCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(TinyGoStatementInspection())
        setupTinyGo(this, myFixture.project)
    }

    private fun prepareTinyGoScheduler(scheduler: Scheduler) {
        val project = myFixture.project
        val tinyGoSettings = TinyGoConfiguration.getInstance(project)
        tinyGoSettings.scheduler = scheduler
        tinyGoSettings.saveState(project)
    }

    fun testBlinkParallelCoroutinesOff() {
        prepareTinyGoScheduler(Scheduler.NONE)
        doTest()
    }

    fun testBlinkParallelCoroutinesOn() {
        prepareTinyGoScheduler(Scheduler.COROUTINES)
        doTest()
    }

    private fun doTest() {
        val testName = getTestName(true)
        testHighlighting("$testName.go")
    }

    override fun getTestDataPath(): String {
        return System.getProperty("user.dir") + "/src/test/testData/inspections/go-statement"
    }
}
