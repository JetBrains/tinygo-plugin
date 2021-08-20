package main

func _() {
    var b interface{}
    var c interface{}
    if <warning descr="TinyGo does not support comparison of interfaces">b == c</warning> {

    }
}
