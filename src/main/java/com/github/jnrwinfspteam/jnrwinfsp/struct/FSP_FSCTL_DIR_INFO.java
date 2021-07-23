package com.github.jnrwinfspteam.jnrwinfsp.struct;

import com.github.jnrwinfspteam.jnrwinfsp.lib.StringUtils;
import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import jnr.ffi.NativeType;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Union;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

public class FSP_FSCTL_DIR_INFO extends Struct {
    public final Struct.Unsigned16 Size = new Unsigned16();
    public final FSP_FSCTL_FILE_INFO FileInfo = inner(new FSP_FSCTL_FILE_INFO(getRuntime()));
    public final DUMMY DUMMYUNIONNAME = inner(new DUMMY(getRuntime()));
    public final Struct.Unsigned8[] FileNameBuf; // initialised in constructor

    public static final class DUMMY extends Union {
        public final Union.Unsigned64 NextOffset = new Unsigned64();
        public final Union.Padding Padding = new Padding(NativeType.UCHAR, 24);

        public DUMMY(Runtime runtime) {
            super(runtime);
        }
    }

    public static Pointered<FSP_FSCTL_DIR_INFO> create(int fileNameSize) {
        var di = new FSP_FSCTL_DIR_INFO(Runtime.getSystemRuntime(), fileNameSize);

        // allocate the necessary memory for the struct
        Pointered<FSP_FSCTL_DIR_INFO> diP = Pointered.allocate(di);

        // initialise every member to zero
        di.Size.set(0);
        di.FileInfo.ReparseTag.set(0);
        di.FileInfo.AllocationSize.set(0);
        di.FileInfo.FileSize.set(0);
        di.FileInfo.CreationTime.set(0);
        di.FileInfo.LastAccessTime.set(0);
        di.FileInfo.LastWriteTime.set(0);
        di.FileInfo.ChangeTime.set(0);
        di.FileInfo.IndexNumber.set(0);
        di.FileInfo.HardLinks.set(0);
        di.FileInfo.EaSize.set(0);

        return diP;
    }

    public void setFileName(byte[] fileNameBytes) {
        if (fileNameBytes.length != FileNameBuf.length)
            throw new IllegalArgumentException("file name size must match the configured length");

        for (int i = 0; i < fileNameBytes.length; i++) {
            this.FileNameBuf[i].set(fileNameBytes[i]);
        }
    }

    private FSP_FSCTL_DIR_INFO(Runtime runtime, int fileNameSize) {
        super(runtime);
        this.FileNameBuf = array(new Unsigned8[fileNameSize]);
    }
}
