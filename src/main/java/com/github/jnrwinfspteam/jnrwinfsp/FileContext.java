package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FileContext extends Struct {
    // TODO
    // maybe let the user specify how this gets encoded/decoded to/from bytes

    public static Pointered<FileContext> of(jnr.ffi.Pointer pointer) {
        return Pointered.wrap(new FileContext(Runtime.getSystemRuntime()), pointer);
    }

    public static Pointered<FileContext> create(Runtime runtime) {
        var fc = new FileContext(runtime);

        // allocate the necessary memory for the struct
        return Pointered.allocate(fc);
    }

    private FileContext(Runtime runtime) {
        super(runtime);
    }
}
