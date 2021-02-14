package org.jetbrains.tinygoplugin.configuration

enum class GarbageCollector(val cmd: String) {
    AUTO_DETECT("Auto detect"),
    NONE("none"),
    LEAKING("leaking"),
    EXTALLOC("extalloc"),
    CONSERVATIVE("conservative")
}

enum class Scheduler(val cmd: String) {
    AUTO_DETECT("Auto detect"),
    NONE("none"),
    COROUTINES("coroutines"),
    TASKS("tasks")
}
