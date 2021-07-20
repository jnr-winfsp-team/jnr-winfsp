package com.github.jnrwinfspteam.jnrwinfsp.lib;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.NulTerminate;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.u_int32_t;

public interface LibWinFsp {

    /**
     * Check whether creating a file system object is possible.
     *
     * @param devicePath The name of the control device for this file system. This must be either
     *                   {@link FSP#FSCTL_DISK_DEVICE_NAME} or {@link FSP#FSCTL_NET_DEVICE_NAME}.
     * @param mountPoint The mount point for the new file system. A value of NULL means that the file system should
     *                   use the next available drive letter counting downwards from Z: as its mount point.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemPreflight(
            @NulTerminate byte[] devicePath,
            @NulTerminate byte[] mountPoint
    );

    /**
     * Create a file system object.
     *
     * @param devicePath   The name of the control device for this file system. This must be either
     *                     {@link FSP#FSCTL_DISK_DEVICE_NAME} or {@link FSP#FSCTL_NET_DEVICE_NAME}.
     * @param volumeParams Volume parameters for the newly created file system.
     * @param _interface   A pointer to the actual operations that actually implement this user mode file system.
     * @param pFileSystem  [out]
     *                     Pointer that will receive the file system object created on successful return from this
     *                     call.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemCreate(
            @NulTerminate byte[] devicePath,
            Pointer /* FSP_FSCTL_VOLUME_PARAMS */ volumeParams,
            Pointer /* FSP_FILE_SYSTEM_INTERFACE */ _interface,
            @Out PointerByReference pFileSystem
    );

    /**
     * Delete a file system object.
     *
     * @param fileSystem The file system object.
     */
    void FspFileSystemDelete(
            Pointer /* FSP_FILE_SYSTEM */ fileSystem
    );

    /**
     * Set the mount point for a file system.
     * <p>
     * This function supports drive letters (X:) or directories as mount points:
     * <ul>
     * <li>Drive letters: Refer to the documentation of the DefineDosDevice Windows API
     * to better understand how they are created.</li>
     * <li>Directories: They can be used as mount points for disk based file systems. They cannot
     * be used for network file systems. This is a limitation that Windows imposes on junctions.</li>
     * </ul>
     *
     * @param fileSystem The file system object.
     * @param mountPoint The mount point for the new file system. A value of NULL means that the file system should
     *                   use the next available drive letter counting downwards from Z: as its mount point.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemSetMountPoint(
            Pointer /* FSP_FILE_SYSTEM */ fileSystem,
            @NulTerminate byte[] mountPoint
    );

    /**
     * Remove the mount point for a file system.
     *
     * @param fileSystem The file system object.
     */
    void FspFileSystemRemoveMountPoint(
            Pointer /* FSP_FILE_SYSTEM */ fileSystem
    );

    /**
     * Start the file system dispatcher.
     * <p>
     * The file system dispatcher is used to dispatch operations posted by the FSD to the user mode
     * file system. Once this call starts executing the user mode file system will start receiving
     * file system requests from the kernel.
     *
     * @param fileSystem  The file system object.
     * @param threadCount The number of threads for the file system dispatcher. A value of 0 will create a default
     *                    number of threads and should be chosen in most cases.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemStartDispatcher(
            Pointer /* FSP_FILE_SYSTEM */ fileSystem,
            long threadCount
    );

    /**
     * Stop the file system dispatcher.
     *
     * @param fileSystem The file system object.
     */
    void FspFileSystemStopDispatcher(
            Pointer /* FSP_FILE_SYSTEM */ fileSystem
    );

    void FspDebugLogSetHandle(Pointer handle);

    void FspFileSystemSetDebugLogF(
            Pointer /* FSP_FILE_SYSTEM */ fileSystem,
            @u_int32_t int debugLog
    );
}