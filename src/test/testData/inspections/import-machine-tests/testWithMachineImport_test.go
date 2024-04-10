package main

import (
    <error descr="Tests that use machine package are yet not supported by TinyGo."><caret>"machine"</error>
    "time"
)

func TestA() {
    led := machine.LED
    led.Configure(machine.PinConfig{Mode: machine.PinOutput})
    time.Sleep(time.Millisecond * 500)
}
