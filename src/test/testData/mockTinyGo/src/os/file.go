// Package os implements a subset of the Go "os" package. See
// https://godoc.org/os for details.
//
// Note that the current implementation is blocking. This limitation should be
// removed in a future version.
package os

import (
	"io"
	"syscall"
)

// Mkdir creates a directory. If the operation fails, it will return an error of
// type *PathError.
func Mkdir(path string, perm FileMode) error {
	fs, suffix := findMount(path)
	if fs == nil {
		return &PathError{"mkdir", path, ErrNotExist}
	}
	err := fs.Mkdir(suffix, perm)
	if err != nil {
		return &PathError{"mkdir", path, err}
	}
	return nil
}

// Remove removes a file or (empty) directory. If the operation fails, it will
// return an error of type *PathError.
func Remove(path string) error {
	fs, suffix := findMount(path)
	if fs == nil {
		return &PathError{"remove", path, ErrNotExist}
	}
	err := fs.Remove(suffix)
	if err != nil {
		return &PathError{"remove", path, err}
	}
	return nil
}

// File represents an open file descriptor.
type File struct {
	handle FileHandle
	name   string
}

// Name returns the name of the file with which it was opened.
func (f *File) Name() string {
	return f.name
}

// OpenFile opens the named file. If the operation fails, the returned error
// will be of type *PathError.
func OpenFile(name string, flag int, perm FileMode) (*File, error) {
	fs, suffix := findMount(name)
	if fs == nil {
		return nil, &PathError{"open", name, ErrNotExist}
	}
	handle, err := fs.OpenFile(suffix, flag, perm)
	if err != nil {
		return nil, &PathError{"open", name, err}
	}
	return &File{name: name, handle: handle}, nil
}

// Open opens the file named for reading.
func Open(name string) (*File, error) {
	return OpenFile(name, O_RDONLY, 0)
}

// Create creates the named file, overwriting it if it already exists.
func Create(name string) (*File, error) {
	return OpenFile(name, O_RDWR|O_CREATE|O_TRUNC, 0666)
}

// Read reads up to len(b) bytes from the File. It returns the number of bytes
// read and any error encountered. At end of file, Read returns 0, io.EOF.
func (f *File) Read(b []byte) (n int, err error) {
	n, err = f.handle.Read(b)
	if err != nil && err != io.EOF {
		err = &PathError{"read", f.name, err}
	}
	return
}

func (f *File) ReadAt(b []byte, off int64) (n int, err error) {
	return 0, ErrNotImplemented
}

// Write writes len(b) bytes to the File. It returns the number of bytes written
// and an error, if any. Write returns a non-nil error when n != len(b).
func (f *File) Write(b []byte) (n int, err error) {
	n, err = f.handle.Write(b)
	if err != nil {
		err = &PathError{"write", f.name, err}
	}
	return
}

// Close closes the File, rendering it unusable for I/O.
func (f *File) Close() (err error) {
	err = f.handle.Close()
	if err != nil {
		err = &PathError{"close", f.name, err}
	}
	return
}

// Readdir is a stub, not yet implemented
func (f *File) Readdir(n int) ([]FileInfo, error) {
	return nil, &PathError{"readdir", f.name, ErrNotImplemented}
}

// Readdirnames is a stub, not yet implemented
func (f *File) Readdirnames(n int) (names []string, err error) {
	return nil, &PathError{"readdirnames", f.name, ErrNotImplemented}
}

// Seek is a stub, not yet implemented
func (f *File) Seek(offset int64, whence int) (ret int64, err error) {
	return 0, &PathError{"seek", f.name, ErrNotImplemented}
}

// Stat is a stub, not yet implemented
func (f *File) Stat() (FileInfo, error) {
	return nil, &PathError{"stat", f.name, ErrNotImplemented}
}

// Sync is a stub, not yet implemented
func (f *File) Sync() error {
	return ErrNotImplemented
}

func (f *File) SyscallConn() (syscall.RawConn, error) {
	return nil, ErrNotImplemented
}

// Fd returns the file handle referencing the open file.
func (f *File) Fd() uintptr {
	panic("unimplemented: os.file.Fd()")
}

const (
	PathSeparator     = '/' // OS-specific path separator
	PathListSeparator = ':' // OS-specific path list separator
)

// IsPathSeparator reports whether c is a directory separator character.
func IsPathSeparator(c uint8) bool {
	return PathSeparator == c
}

// PathError records an error and the operation and file path that caused it.
type PathError struct {
	Op   string
	Path string
	Err  error
}

func (e *PathError) Error() string {
	return e.Op + " " + e.Path + ": " + e.Err.Error()
}

const (
	O_RDONLY int = syscall.O_RDONLY
	O_WRONLY int = syscall.O_WRONLY
	O_RDWR   int = syscall.O_RDWR
	O_APPEND int = syscall.O_APPEND
	O_CREATE int = syscall.O_CREAT
	O_EXCL   int = syscall.O_EXCL
	O_SYNC   int = syscall.O_SYNC
	O_TRUNC  int = syscall.O_TRUNC
)

// Stat is a stub, not yet implemented
func Stat(name string) (FileInfo, error) {
	return nil, &PathError{"stat", name, ErrNotImplemented}
}

// Lstat is a stub, not yet implemented
func Lstat(name string) (FileInfo, error) {
	return nil, &PathError{"lstat", name, ErrNotImplemented}
}

// Getwd is a stub (for now), always returning an empty string
func Getwd() (string, error) {
	return "", nil
}

// Readlink is a stub (for now), always returning the string it was given
func Readlink(name string) (string, error) {
	return name, nil
}

// TempDir is a stub (for now), always returning the string "/tmp"
func TempDir() string {
	return "/tmp"
}
