package main

<caret>func TestA() {
    led := machine.LED
    led.Configure(machine.PinConfig{Mode: machine.PinOutput})
}
