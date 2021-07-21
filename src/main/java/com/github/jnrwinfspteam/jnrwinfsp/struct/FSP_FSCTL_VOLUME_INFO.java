package com.github.jnrwinfspteam.jnrwinfsp.struct;

import com.github.jnrwinfspteam.jnrwinfsp.lib.StringUtils;
import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

public class FSP_FSCTL_VOLUME_INFO extends Struct {
    public final Struct.Unsigned64 TotalSize = new Unsigned64();
    public final Struct.Unsigned64 FreeSize = new Unsigned64();
    public final Struct.Unsigned16 VolumeLabelLength = new Unsigned16();
    public final Struct.Unsigned8[] VolumeLabel = array(new Unsigned8[32 * StringUtils.CS_BYTES_PER_CHAR]);

    private FSP_FSCTL_VOLUME_INFO(Runtime runtime) {
        super(runtime);
    }

    public static Pointered<FSP_FSCTL_VOLUME_INFO> of(jnr.ffi.Pointer pointer) {
        return Pointered.wrap(new FSP_FSCTL_VOLUME_INFO(Runtime.getSystemRuntime()), pointer);
    }

    public void setVolumeLabel(java.lang.String label) {
        try {
            ByteBuffer bytes = StringUtils.getEncoder().reset().encode(CharBuffer.wrap(label));
            final int byteLength = bytes.remaining();

            if (byteLength > VolumeLabel.length - StringUtils.CS_BYTES_PER_CHAR)
                throw new IllegalArgumentException("label is too large");

            int i = 0;
            for (; i < byteLength; i++) {
                this.VolumeLabel[i].set(bytes.get(i));
            }
            this.VolumeLabel[i].set((byte) '\0');
            this.VolumeLabelLength.set(byteLength);
        } catch (CharacterCodingException cce) {
            throw new RuntimeException(cce);
        }
    }
}
