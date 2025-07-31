package org.jetbrains.tinygoplugin.configuration

enum class GarbageCollector(val cmd: String) {
    AUTO_DETECT("auto"),
    NONE("none"),
    LEAKING("leaking"),
    EXTALLOC("extalloc"),
    CONSERVATIVE("conservative");

    override fun toString(): String = cmd
}

enum class Scheduler(val cmd: String) {
    AUTO_DETECT("auto"),
    NONE("none"),
    ASYNCIFY("asyncify"),
    CORES("cores"),
    THREADS("cores"),
    TASKS("tasks");

    override fun toString(): String = cmd
}
