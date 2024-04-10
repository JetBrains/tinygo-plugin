package main

import <error descr="Tests that use machine package are yet not supported by TinyGo."><caret>"machine"</error>

func TestA() {
    led := machine.LED
    led.Configure(machine.PinConfig{Mode: machine.PinOutput})
}
