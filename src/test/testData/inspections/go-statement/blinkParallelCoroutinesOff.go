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
    <error descr="Scheduler is set to None, go statements are not supported">go blink(machine.LED)</error>
}
