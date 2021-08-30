package com.github.jnrwinfspteam.jnrwinfsp;

import java.nio.file.Path;

public interface Mountable {

    /**
     * Mounts a local WinFsp drive on the given mount point. If the mount point is null, then
     * it will be the next available drive letter counting downwards from Z:
     *
     * @param mountPoint A file path (may be null)
     * @param options    Mount options
     * @throws MountException       If some problem occurs during mounting, or if the drive is already mounted
     * @throws NullPointerException If {@code fsCaseOption} is null
     */
    void mountLocalDrive(Path mountPoint, MountOptions options) throws MountException;

    default void mountLocalDrive(Path mountPoint) throws MountException {
        mountLocalDrive(mountPoint, new MountOptions());
    }

    /**
     * Unmounts the currently mounted WinFsp drive. Will not have any effect if there isn't any currently
     * mounted drive.
     */
    void unmountLocalDrive();
}
