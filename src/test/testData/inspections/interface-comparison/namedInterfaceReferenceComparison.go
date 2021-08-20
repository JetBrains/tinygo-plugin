package main

type iface interface {
    foo() float64
}

type A struct {
    bar float64
}
type B struct {
    baz float64
}

func (a A) foo() float64 {
    return a.bar
}

func (b B) foo() float64 {
    return b.baz
}

func _() {
    var a iface = A{bar: 5.0}
    var b = nil
    if a == b {

    }
}
