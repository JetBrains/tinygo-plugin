package main

import (
<caret>	"time"
)

func TestA() {
    led := machine.LED
    led.Configure(machine.PinConfig{Mode: machine.PinOutput})
    time.Sleep(time.Millisecond * 500)
}
