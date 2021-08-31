package task

import (
	"unsafe"
)

// Task is a state of goroutine for scheduling purposes.
type Task struct {
	// Next is a field which can be used to make a linked list of tasks.
	Next *Task

	// Ptr is a field which can be used for storing a pointer.
	Ptr unsafe.Pointer

	// Data is a field which can be used for storing state information.
	Data uint64

	// state is the underlying running state of the task.
	state state
}

// getGoroutineStackSize is a compiler intrinsic that returns the stack size for
// the given function and falls back to the default stack size. It is replaced
// with a load from a special section just before codegen.
func getGoroutineStackSize(fn uintptr) uintptr
