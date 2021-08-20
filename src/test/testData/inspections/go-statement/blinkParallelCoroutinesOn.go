package main

import (
    "machine"
    "time"
)

func blink(led machine.Pin) {
    led.Configure(machine.PinConfig{Mode: machine.PinOutput})
    	for {
    		led.Low()
    		time.Sleep(time.Millisecond * 500)

    		led.High()
    		time.Sleep(time.Millisecond * 500)
    	}
}

func main() {
    go blink(machine.LED)
}
