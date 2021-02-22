package org.jetbrains.tinygoplugin.runconfig

interface ConfigurationType {
    val command: String
}

class RunConfiguration : ConfigurationType {
    override val command: String = "run"
}

class FlashConfiguration : ConfigurationType {
    override val command: String = "flash"
}

class TestConfiguration : ConfigurationType {
    override val command: String = "test"
}
