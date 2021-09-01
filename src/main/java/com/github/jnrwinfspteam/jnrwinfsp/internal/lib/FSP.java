package com.github.jnrwinfspteam.jnrwinfsp.internal.lib;

public final class FSP {
    public static final String FSCTL_DISK_DEVICE_NAME = "WinFsp.Disk";
    public static final String FSCTL_NET_DEVICE_NAME = "WinFsp.Net";

    public static final int FSCTL_VOLUME_NAME_SIZE = 64;
    public static final int FSCTL_VOLUME_PREFIX_SIZE = 192;
    public static final int FSCTL_VOLUME_FSNAME_SIZE = 16;
    public static final int FSCTL_VOLUME_NAME_SIZEMAX = FSCTL_VOLUME_NAME_SIZE + FSCTL_VOLUME_PREFIX_SIZE;

    public static final int FsctlTransactKindCount = 22; // FIXME this corresponds to the last value of a C enum

    private FSP() {
        // not instantiable
    }
}