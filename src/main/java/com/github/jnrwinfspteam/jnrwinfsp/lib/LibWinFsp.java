package com.github.jnrwinfspteam.jnrwinfsp.lib;

import jnr.ffi.Pointer;
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
            char[] devicePath,
            char[] mountPoint
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
            char[] devicePath,
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
            char[] mountPoint
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

    /**
     * Modify security descriptor.
     * <p>
     * This is a helper for implementing the SetSecurity operation.
     *
     * @param inputDescriptor         The input security descriptor to be modified.
     * @param securityInformation     Describes what parts of the InputDescriptor should be modified. This should contain
     *                                the same value passed to the SetSecurity SecurityInformation parameter.
     * @param pModificationDescriptor Describes the modifications to apply to the InputDescriptor. This should contain
     *                                the same value passed to the SetSecurity ModificationDescriptor parameter.
     * @param ppSecurityDescriptor    [out]
     *                                Pointer to a memory location that will receive the resulting security descriptor.
     *                                This security descriptor can be later freed using FspDeleteSecurityDescriptor.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspSetSecurityDescriptor(
            Pointer /* SECURITY_DESCRIPTOR */ inputDescriptor,
            @u_int32_t int securityInformation,
            Pointer /* SECURITY_DESCRIPTOR */ pModificationDescriptor,
            @Out PointerByReference /* PSECURITY_DESCRIPTOR */ ppSecurityDescriptor
    );

    /**
     * Add directory information to a buffer.
     * <p>
     * This is a helper for implementing the ReadDirectory operation.
     *
     * @param dirInfo           The directory information to add. A value of NULL acts as an EOF marker for a ReadDirectory
     *                          operation.
     * @param buffer            Pointer to a buffer that will receive the results of the read operation. This should contain
     *                          the same value passed to the ReadDirectory Buffer parameter.
     * @param length            Length of data to read. This should contain the same value passed to the ReadDirectory
     *                          Length parameter.
     * @param pBytesTransferred [out]
     *                          Pointer to a memory location that will receive the actual number of bytes read. This should
     *                          contain the same value passed to the ReadDirectory PBytesTransferred parameter.
     *                          FspFileSystemAddDirInfo uses the value pointed by this parameter to track how much of the
     *                          buffer has been used so far.
     * @return TRUE if the directory information was added, FALSE if there was not enough space to add it.
     */
    boolean FspFileSystemAddDirInfo(
            Pointer /* FSP_FSCTL_DIR_INFO */ dirInfo,
            Pointer /* VOID */ buffer,
            @u_int32_t int length,
            Pointer /* ULONG */ pBytesTransferred
    );

    @u_int32_t
    int FspNtStatusFromWin32(@u_int32_t int error);
}