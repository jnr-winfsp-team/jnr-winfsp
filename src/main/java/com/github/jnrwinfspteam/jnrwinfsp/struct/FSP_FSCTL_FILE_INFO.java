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

    public static Pointered<FSP_FSCTL_FILE_INFO> create() {
        var fi = new FSP_FSCTL_FILE_INFO(Runtime.getSystemRuntime());

        // allocate the necessary memory for the struct
        Pointered<FSP_FSCTL_FILE_INFO> fiP = Pointered.allocate(fi);

        // initialise every member to zero
        fi.FileAttributes.set(0);
        fi.ReparseTag.set(0);
        fi.AllocationSize.set(0);
        fi.FileSize.set(0);
        fi.CreationTime.set(0);
        fi.LastAccessTime.set(0);
        fi.LastWriteTime.set(0);
        fi.ChangeTime.set(0);
        fi.IndexNumber.set(0);
        fi.HardLinks.set(0);
        fi.EaSize.set(0);

        return fiP;
    }

    FSP_FSCTL_FILE_INFO(Runtime runtime) {
        super(runtime);
    }
}