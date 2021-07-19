package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_VOLUME_PARAMS;

import java.nio.file.Path;

public interface Mountable {

    /**
     * Mounts a local WinFsp drive on the given mount point. If the mount point is null, then
     * it will be the next available drive letter counting downwards from Z:
     *
     * @param mountPoint A file path (may be null)
     * @param debug      If true, then debugging output will be printed to the standard error stream
     * @throws MountException If some problem occurs during mounting, or if the drive is already mounted
     */
    void mountLocalDrive(Path mountPoint, boolean debug) throws MountException;

    default void mountLocalDrive(Path mountPoint) throws MountException {
        mountLocalDrive(mountPoint, false);
    }

    /**
     * Unmounts the currently mounted WinFsp drive. Will not have any effect if there isn't any currently
     * mounted drive.
     */
    void unmountLocalDrive();
}
