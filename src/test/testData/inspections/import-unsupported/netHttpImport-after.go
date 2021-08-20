package main

import (
<caret>	"time"
)

func _() {
    _, _ = http.Get("http://foo.bar")
    time.Sleep(time.Millisecond * 500)
}
