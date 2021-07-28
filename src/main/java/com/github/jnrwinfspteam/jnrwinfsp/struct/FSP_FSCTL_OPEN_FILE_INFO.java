package com.github.jnrwinfspteam.jnrwinfsp.struct;

import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FSP_FSCTL_OPEN_FILE_INFO extends Struct {

    public final FSP_FSCTL_FILE_INFO FileInfo = inner(new FSP_FSCTL_FILE_INFO(getRuntime()));
    public final Struct.Pointer NormalizedName = new Pointer(); /* PWSTR */
    public final Struct.Unsigned16 NormalizedNameSize = new Unsigned16();

    public static Pointered<FSP_FSCTL_OPEN_FILE_INFO> of(jnr.ffi.Pointer pointer) {
        return Pointered.wrap(new FSP_FSCTL_OPEN_FILE_INFO(Runtime.getSystemRuntime()), pointer);
    }

    private FSP_FSCTL_OPEN_FILE_INFO(Runtime runtime) {
        super(runtime);
    }
}
