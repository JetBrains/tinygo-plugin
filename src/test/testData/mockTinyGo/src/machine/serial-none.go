// +build baremetal,serial.none

package machine

// Serial is a null device: writes to it are ignored.
var Serial = NullSerial{}
