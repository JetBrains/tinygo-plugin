package org.jetbrains.tinygoplugin.configuration

import com.google.gson.Gson
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jetbrains.tinygoplugin.testFramework.setupTinyGo
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import java.io.File

class TinyGoTargetJsonTest : BasePlatformTestCase() {
    lateinit var mySetting: TinyGoConfiguration
    override fun setUp() {
        super.setUp()
        val pathToCustomTarget =
            System.getProperty("user.dir") + "/src/test/testData/configuration/my-custom-board.json"

        mySetting = setupTinyGo(this, myFixture.project) {
            it.targetPlatform = pathToCustomTarget
            it.goOS = "linux"
            it.goArch = "arm"
            it.gc = GarbageCollector.CONSERVATIVE
            it.scheduler = Scheduler.COROUTINES
            it.goTags = "myarch baremetal linux arm mycpu myboard tag anotherTag"
        }
    }

    private fun testTargetObj(target: TinyGoTarget) {
        TestCase.assertEquals("linux", target.goOS)
        TestCase.assertEquals("arm", target.goArch)
        TestCase.assertEquals("conservative", target.gc)
        TestCase.assertEquals("coroutines", target.scheduler)
        TestCase.assertEquals(123, target.defaultStackSize)
        TestCase.assertEquals("mycpu", target.cpu)
        TestCase.assertEquals("uart", target.serial)
        TestCase.assertEquals("flashutil -c myboard -p mycontroller -P {port}", target.flashCommand)
        TestCase.assertEquals(
            setOf("acm:2341:0043", "acm:2341:0001", "acm:2a03:0043", "acm:2341:0243"),
            target.serialPort
        )
        TestCase.assertEquals(
            setOf("myboardsim", "-m", "myboard", "-f", "123456789"),
            target.emulator
        )
        TestCase.assertEquals(
            setOf("myarch", "baremetal", "linux", "arm", "mycpu", "myboard", "tag", "anotherTag"),
            target.buildTags
        )
    }

    fun testDependencyResolver() {
        val target = createTargetWrapper(
            TinyGoPropertiesWrapper(
                object : ConfigurationProvider<TinyGoConfiguration> {
                    override val tinyGoSettings: TinyGoConfiguration = mySetting
                }
            )
        ) ?: return

        testTargetObj(target)
    }

    fun testDeserialized() {
        val target = createTargetWrapper(
            TinyGoPropertiesWrapper(
                object : ConfigurationProvider<TinyGoConfiguration> {
                    override val tinyGoSettings: TinyGoConfiguration = mySetting
                }
            )
        ) ?: return

        val testFile = File.createTempFile(
            "target",
            ".json",
            File(System.getProperty("user.dir") + "/src/test/testData/configuration/")
        )
        testFile.writeText(target.serialize())

        val newTarget = Gson().fromJson(testFile.readText(), TinyGoTarget::class.java)
        testTargetObj(newTarget)
    }
}
