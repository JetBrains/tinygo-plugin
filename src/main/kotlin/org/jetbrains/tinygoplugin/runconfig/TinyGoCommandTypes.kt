package org.jetbrains.tinygoplugin.runconfig

interface TinyGoCommandType {
    val command: String
}

class TinyGoRunCommand : TinyGoCommandType {
    override val command: String = "run"
}

class TinyGoFlashCommand : TinyGoCommandType {
    override val command: String = "flash"
}

class TinyGoTestCommand : TinyGoCommandType {
    override val command: String = "test"
}
