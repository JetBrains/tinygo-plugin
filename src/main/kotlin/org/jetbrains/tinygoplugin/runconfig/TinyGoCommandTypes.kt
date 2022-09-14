package org.jetbrains.tinygoplugin.runconfig

interface TinyGoCommandType {
    val command: String
}

object TinyGoBuildCommand : TinyGoCommandType {
    override val command: String = "build"
}

object TinyGoRunCommand : TinyGoCommandType {
    override val command: String = "run"
}

object TinyGoFlashCommand : TinyGoCommandType {
    override val command: String = "flash"
}

object TinyGoTestCommand : TinyGoCommandType {
    override val command: String = "test"
}
