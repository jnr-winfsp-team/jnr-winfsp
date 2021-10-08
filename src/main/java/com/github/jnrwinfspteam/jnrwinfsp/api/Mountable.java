package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.nio.file.Path;

public interface Mountable {

    /**
     * Mounts a local WinFsp drive as a Windows Service.
     * <ul>
     *     <li>On service start, the function {@link #mountLocalDrive(Path, MountOptions)} will be called with the
     *     given arguments</li>
     *     <li>On service stop, the function {@link #unmountLocalDrive()} will be called</li>
     * </ul>
     *
     * @param serviceName The name of the service
     * @param mountPoint  A file path (may be null)
     * @param options     Mount options
     * @throws MountException       If some problem occurs during mounting, or if the drive is already mounted
     * @throws NullPointerException If {@code serviceName} or {@code options} is null
     */
    void mountLocalDriveAsAService(String serviceName, Path mountPoint, MountOptions options) throws MountException;

    default void mountLocalDriveAsAService(String serviceName, Path mountPoint) throws MountException {
        mountLocalDriveAsAService(serviceName, mountPoint, new MountOptions());
    }

    /**
     * Mounts a local WinFsp drive on the given mount point. If the mount point is null, then
     * it will be the next available drive letter counting downwards from Z:
     *
     * @param mountPoint A file path (may be null)
     * @param options    Mount options
     * @throws MountException       If some problem occurs during mounting, or if the drive is already mounted
     * @throws NullPointerException If {@code options} is null
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
