package com.github.jnrwinfspteam.jnrwinfsp.struct;

import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FSP_FSCTL_FILE_INFO extends Struct {

    public final Struct.Unsigned32 FileAttributes = new Unsigned32();
    public final Struct.Unsigned32 ReparseTag = new Unsigned32();
    public final Struct.Unsigned64 AllocationSize = new Unsigned64();
    public final Struct.Unsigned64 FileSize = new Unsigned64();
    public final Struct.Unsigned64 CreationTime = new Unsigned64();
    public final Struct.Unsigned64 LastAccessTime = new Unsigned64();
    public final Struct.Unsigned64 LastWriteTime = new Unsigned64();
    public final Struct.Unsigned64 ChangeTime = new Unsigned64();
    public final Struct.Unsigned64 IndexNumber = new Unsigned64();
    public final Struct.Unsigned32 HardLinks = new Unsigned32();      /* unimplemented: set to 0 */
    public final Struct.Unsigned32 EaSize = new Unsigned32();

    public static Pointered<FSP_FSCTL_FILE_INFO> of(jnr.ffi.Pointer pointer) {
        return Pointered.wrap(new FSP_FSCTL_FILE_INFO(Runtime.getSystemRuntime()), pointer);
    }

    FSP_FSCTL_FILE_INFO(Runtime runtime) {
        super(runtime);
    }
}