// +build gc.leaking

package runtime

// This GC implementation is the simplest useful memory allocator possible: it
// only allocates memory and never frees it. For some constrained systems, it
// may be the only memory allocator possible.

import (
	"unsafe"
)

// Ever-incrementing pointer: no memory is freed.
var heapptr = heapStart

func alloc(size uintptr) unsafe.Pointer {
	// TODO: this can be optimized by not casting between pointers and ints so
	// much. And by using platform-native data types (e.g. *uint8 for 8-bit
	// systems).
	size = align(size)
	addr := heapptr
	heapptr += size
	for heapptr >= heapEnd {
		// Try to increase the heap and check again.
		if growHeap() {
			continue
		}
		// Failed to make the heap bigger, so we must really be out of memory.
		runtimePanic("out of memory")
	}
	for i := uintptr(0); i < uintptr(size); i += 4 {
		ptr := (*uint32)(unsafe.Pointer(addr + i))
		*ptr = 0
	}
	return unsafe.Pointer(addr)
}

func free(ptr unsafe.Pointer) {
	// Memory is never freed.
}

func GC() {
	// No-op.
}

func KeepAlive(x interface{}) {
	// Unimplemented. Only required with SetFinalizer().
}

func SetFinalizer(obj interface{}, finalizer interface{}) {
	// Unimplemented.
}

func initHeap() {
	// Nothing to initialize.
}

// setHeapEnd sets a new (larger) heapEnd pointer.
func setHeapEnd(newHeapEnd uintptr) {
	// This "heap" is so simple that simply assigning a new value is good
	// enough.
	heapEnd = newHeapEnd
}
