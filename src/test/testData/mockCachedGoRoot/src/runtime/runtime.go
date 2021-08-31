package runtime

import (
	"unsafe"
)

const Compiler = "tinygo"

// The compiler will fill this with calls to the initialization function of each
// package.
func initAll()

//go:linkname callMain main.main
func callMain()

func GOMAXPROCS(n int) int {
	// Note: setting GOMAXPROCS is ignored.
	return 1
}

func GOROOT() string {
	// TODO: don't hardcode but take the one at compile time.
	return "/usr/local/go"
}

// This is the default set of arguments, if nothing else has been set.
// This may be overriden by modifying this global at runtime init (for example,
// on Linux where there are real command line arguments).
var args = []string{"/proc/self/exe"}

//go:linkname os_runtime_args os.runtime_args
func os_runtime_args() []string {
	return args
}

// Copy size bytes from src to dst. The memory areas must not overlap.
// Calls to this function are converted to LLVM intrinsic calls such as
// llvm.memcpy.p0i8.p0i8.i32(dst, src, size, false).
func memcpy(dst, src unsafe.Pointer, size uintptr)

// Copy size bytes from src to dst. The memory areas may overlap and will do the
// correct thing.
// Calls to this function are converted to LLVM intrinsic calls such as
// llvm.memmove.p0i8.p0i8.i32(dst, src, size, false).
func memmove(dst, src unsafe.Pointer, size uintptr)

// Set the given number of bytes to zero.
// Calls to this function are converted to LLVM intrinsic calls such as
// llvm.memset.p0i8.i32(ptr, 0, size, false).
func memzero(ptr unsafe.Pointer, size uintptr)

//export strlen
func strlen(ptr unsafe.Pointer) uintptr

// Compare two same-size buffers for equality.
func memequal(x, y unsafe.Pointer, n uintptr) bool {
	for i := uintptr(0); i < n; i++ {
		cx := *(*uint8)(unsafe.Pointer(uintptr(x) + i))
		cy := *(*uint8)(unsafe.Pointer(uintptr(y) + i))
		if cx != cy {
			return false
		}
	}
	return true
}

func nanotime() int64 {
	return ticksToNanoseconds(ticks())
}

// timeOffset is how long the monotonic clock started after the Unix epoch. It
// should be a positive integer under normal operation or zero when it has not
// been set.
var timeOffset int64

//go:linkname now time.now
func now() (sec int64, nsec int32, mono int64) {
	mono = nanotime()
	sec = (mono + timeOffset) / (1000 * 1000 * 1000)
	nsec = int32((mono + timeOffset) - sec*(1000*1000*1000))
	return
}

// AdjustTimeOffset adds the given offset to the built-in time offset. A
// positive value adds to the time (skipping some time), a negative value moves
// the clock into the past.
func AdjustTimeOffset(offset int64) {
	// TODO: do this atomically?
	timeOffset += offset
}

// Copied from the Go runtime source code.
//go:linkname os_sigpipe os.sigpipe
func os_sigpipe() {
	runtimePanic("too many writes on closed pipe")
}
