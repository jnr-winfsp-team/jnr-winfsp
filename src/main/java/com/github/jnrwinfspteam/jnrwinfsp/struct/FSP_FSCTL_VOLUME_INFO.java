package com.github.jnrwinfspteam.jnrwinfsp.struct;

import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FSP_FSCTL_VOLUME_INFO extends Struct {
    public final Struct.Unsigned64 TotalSize = new Unsigned64();
    public final Struct.Unsigned64 FreeSize = new Unsigned64();
    public final Struct.Unsigned16 VolumeLabelLength = new Unsigned16();
    public final Struct.Unsigned16[] VolumeLabel = array(new Struct.Unsigned16[32]);

    private FSP_FSCTL_VOLUME_INFO(Runtime runtime) {
        super(runtime);
    }

    public static Pointered<FSP_FSCTL_VOLUME_INFO> of(jnr.ffi.Pointer pointer) {
        return Pointered.wrap(new FSP_FSCTL_VOLUME_INFO(Runtime.getSystemRuntime()), pointer);
    }

    public void setVolumeLabel(java.lang.String label) {
        if (label.length() > 31)
            throw new IllegalArgumentException("label is too large");

        int i = 0;
        for (; i < label.length(); i++) {
            this.VolumeLabel[i].set(label.charAt(i));
        }
        this.VolumeLabel[i].set('\0');
        this.VolumeLabelLength.set(label.length());
    }
}
