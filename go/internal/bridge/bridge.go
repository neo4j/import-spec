package bridge

/*
// This comment block acts as a configuration file for the C compiler
// It must be directly above the C import and point to the header and shared library files

#cgo darwin,arm64 CFLAGS: -I${SRCDIR}/lib/macos-arm64
#cgo darwin,arm64 LDFLAGS: -Wl,-force_load,${SRCDIR}/lib/macos-arm64/libgraphdatamodel.a -framework Foundation -lstdc++

#cgo linux,arm64 CFLAGS: -I${SRCDIR}/lib/linux-arm64
#cgo linux,arm64 LDFLAGS: ${SRCDIR}/lib/linux-arm64/libgraphdatamodel.a -lstdc++ -lm -lpthread -ldl

#cgo linux,amd64 CFLAGS: -I${SRCDIR}/lib/linux-amd64
#cgo linux,amd64 LDFLAGS: ${SRCDIR}/lib/linux-amd64/libgraphdatamodel.a -lstdc++ -lm -lpthread -ldl

#include <stdlib.h>
#include "libgraphdatamodel_api.h"
*/
import "C"
import (
	"encoding/json"
	"fmt"
	"unsafe"
)

type Resp struct {
	Data   string `json:"data"`
	ErrMsg string `json:"error"`
}

type Op string

const (
	Migrate  Op = "Migrate"
	Validate Op = "Validate"
)

func Call(op Op, inputs ...string) (string, error) {
	if len(inputs) == 0 {
		return "", fmt.Errorf("empty input provided")
	}
	for i, input := range inputs {
		if len(input) == 0 {
			return "", fmt.Errorf("empty input provided [%d]", i)
		}
	}

	// Data sent across the Go/Kotlin boundary must be converted to a C-compatible format hence
	// we are reduced to addresses/pointers and primitives (int, float). These primitives must
	// be C-compatible, e.g. C strings rather than Go strings. These C-compatible primitives are
	// allocated on the C heap, meaning they are out of scope of the Go GC. Hence, we must ensure
	// they are freed once finished
	inputPtrs := make([]unsafe.Pointer, len(inputs))
	for i, input := range inputs {
		cStr := C.CString(input)
		defer C.free(unsafe.Pointer(cStr))
		inputPtrs[i] = unsafe.Pointer(cStr)
	}

	// To simplify memory management on the Kotlin side we create an output buffer, still under
	// the scope of the Go GC, for the Kotlin library to write a response to. We must also provide
	// the maximum buffer size to avoid overflows when the Kotlin library writes the output
	buf := make([]byte, 2*len(inputs[0]))
	outPtr := unsafe.Pointer(&buf[0])

	bufSize, err := callBridge(op, inputPtrs, outPtr, C.int(len(buf)))
	if err != nil {
		return "", err
	}

	var resp Resp
	if err := json.Unmarshal(buf[:bufSize], &resp); err != nil {
		return "", fmt.Errorf("failed to parse result: %w", err)
	}
	if resp.ErrMsg != "" {
		return "", fmt.Errorf("received error from library: %s", resp.ErrMsg)
	}

	return resp.Data, nil
}

func callBridge(op Op, input []unsafe.Pointer, out unsafe.Pointer, outLen C.int) (int, error) {
	var res int
	switch op {
	case Migrate:
		res = int(C.migrate(input[0], input[1], input[2], input[3], out, outLen))
	case Validate:
		res = int(C.validate(input[0], out, outLen))
	default:
		return -1, fmt.Errorf("unknown bridge call: %s", op)
	}

	if res < 0 {
		return res, fmt.Errorf("bridge error (buffer too small or internal failure): code %d", res)
	}
	return res, nil
}
