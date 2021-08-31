// +build baremetal

package runtime

import (
	"unsafe"
)

//go:extern _heap_start
var heapStartSymbol [0]byte

//go:extern _heap_end
var heapEndSymbol [0]byte

//go:extern _globals_start
var globalsStartSymbol [0]byte

//go:extern _globals_end
var globalsEndSymbol [0]byte

//go:extern _stack_top
var stackTopSymbol [0]byte

var (
	heapStart    = uintptr(unsafe.Pointer(&heapStartSymbol))
	heapEnd      = uintptr(unsafe.Pointer(&heapEndSymbol))
	globalsStart = uintptr(unsafe.Pointer(&globalsStartSymbol))
	globalsEnd   = uintptr(unsafe.Pointer(&globalsEndSymbol))
	stackTop     = uintptr(unsafe.Pointer(&stackTopSymbol))
)

// growHeap tries to grow the heap size. It returns true if it succeeds, false
// otherwise.
func growHeap() bool {
	// On baremetal, there is no way the heap can be grown.
	return false
}

//export malloc
func libc_malloc(size uintptr) unsafe.Pointer {
	return alloc(size)
}

//export free
func libc_free(ptr unsafe.Pointer) {
	free(ptr)
}

//go:linkname syscall_Exit syscall.Exit
func syscall_Exit(code int) {
	abort()
}

const baremetal = true
