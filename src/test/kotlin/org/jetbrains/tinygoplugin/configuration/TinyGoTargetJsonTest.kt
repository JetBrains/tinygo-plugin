package org.jetbrains.tinygoplugin.configuration

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.tinygoplugin.testFramework.setupTinyGo
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper

class TinyGoTargetJsonTest : BasePlatformTestCase() {
    private val testDataPath = System.getProperty("user.dir") + "/src/test/testData/configuration"

    private fun configure(targetFile: String): TinyGoConfiguration =
        setupTinyGo(this, myFixture.project) {
            it.targetPlatform = "$testDataPath/$targetFile"
            it.goOS = "linux"
            it.goArch = "arm"
            it.gc = GarbageCollector.CONSERVATIVE
            it.scheduler = Scheduler.ASYNCIFY
            it.goTags = "myarch baremetal linux arm mycpu myboard tag anotherTag"
        }

    private fun createTarget(settings: TinyGoConfiguration): TinyGoTarget {
        val target =
            runBlocking {
                createTargetWrapper(
                    TinyGoPropertiesWrapper(
                        object : ConfigurationProvider<TinyGoConfiguration> {
                            override val tinyGoSettings: TinyGoConfiguration = settings
                        },
                    ),
                )
            }
        TestCase.assertNotNull("createTargetWrapper returned no target", target)
        return target!!
    }

    private fun testTargetObj(target: TinyGoTarget) {
        TestCase.assertEquals("linux", target.goOS)
        TestCase.assertEquals("arm", target.goArch)
        TestCase.assertEquals("conservative", target.gc)
        TestCase.assertEquals("asyncify", target.scheduler)
        TestCase.assertEquals(123, target.defaultStackSize)
        TestCase.assertEquals("mycpu", target.cpu)
        TestCase.assertEquals("uart", target.serial)
        TestCase.assertEquals("flashutil -c myboard -p mycontroller -P {port}", target.flashCommand)
        TestCase.assertEquals(
            setOf("acm:2341:0043", "acm:2341:0001", "acm:2a03:0043", "acm:2341:0243"),
            target.serialPort,
        )
        TestCase.assertEquals(
            "myboardsim -m myboard -f 123456789",
            target.emulator,
        )
        TestCase.assertEquals(
            setOf("myarch", "baremetal", "linux", "arm", "mycpu", "myboard", "tag", "anotherTag"),
            target.buildTags,
        )
        TestCase.assertEquals(JsonParser.parseString("""["MYBOOT"]"""), target.flashVolume)
    }

    fun testDependencyResolver() {
        testTargetObj(createTarget(configure("my-custom-board.json")))
    }

    fun testDeserialized() {
        val target = createTarget(configure("my-custom-board.json"))
        testTargetObj(Gson().fromJson(target.serialize(), TinyGoTarget::class.java))
    }

    fun testVolumeNameAsString() {
        val target = createTarget(configure("my-old-board.json"))
        TestCase.assertEquals(JsonParser.parseString("\"OLDBOOT\""), target.flashVolume)
        TestCase.assertTrue(target.serialize().contains("\"msd-volume-name\": \"OLDBOOT\""))
    }
}
