package main

import "machine"

func _() {
    led := machine.LED
    led.Configure(machine.PinConfig{Mode: machine.PinOutput})
}
