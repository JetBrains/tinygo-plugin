package main

import (
    <error descr="net/http does not compile with TinyGo. For more details about support for this package see net/http at tinygo.org">"net/http"<caret></error>
    "time"
)

func _() {
    _, _ = http.Get("http://foo.bar")
    time.Sleep(time.Millisecond * 500)
}
