package com.github.jnrwinfspteam.jnrwinfsp.internal.lib;

import com.github.jnrwinfspteam.jnrwinfsp.internal.util.WinPathUtils;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.size_t;
import jnr.ffi.types.u_int32_t;
import jnr.ffi.types.u_int8_t;

public interface LibWinFsp {

    static final LibWinFsp INSTANCE = LibraryLoader.create(LibWinFsp.class)
            .library(WinPathUtils.getWinFspPath())
            .failImmediately()
            .load();

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
            byte[] devicePath,
            byte[] mountPoint
    );

    /**
     * Create a file system object.
     *
     * @param devicePath    The name of the control device for this file system. This must be either
     *                      {@link FSP#FSCTL_DISK_DEVICE_NAME} or {@link FSP#FSCTL_NET_DEVICE_NAME}.
     * @param pVolumeParams Volume parameters for the newly created file system.
     * @param pInterface    A pointer to the actual operations that actually implement this user mode file system.
     * @param ppFileSystem  [out]
     *                      Pointer that will receive the file system object created on successful return from this
     *                      call.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemCreate(
            byte[] devicePath,
            Pointer /* FSP_FSCTL_VOLUME_PARAMS */ pVolumeParams,
            Pointer /* FSP_FILE_SYSTEM_INTERFACE */ pInterface,
            @Out PointerByReference ppFileSystem
    );

    /**
     * Delete a file system object.
     *
     * @param pFileSystem The file system object.
     */
    void FspFileSystemDelete(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem
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
     * @param pFileSystem The file system object.
     * @param mountPoint  The mount point for the new file system. A value of NULL means that the file system should
     *                    use the next available drive letter counting downwards from Z: as its mount point.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemSetMountPoint(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
            byte[] mountPoint
    );

    /**
     * Remove the mount point for a file system.
     *
     * @param pFileSystem The file system object.
     */
    void FspFileSystemRemoveMountPoint(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem
    );

    /**
     * Start the file system dispatcher.
     * <p>
     * The file system dispatcher is used to dispatch operations posted by the FSD to the user mode
     * file system. Once this call starts executing the user mode file system will start receiving
     * file system requests from the kernel.
     *
     * @param pFileSystem The file system object.
     * @param threadCount The number of threads for the file system dispatcher. A value of 0 will create a default
     *                    number of threads and should be chosen in most cases.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemStartDispatcher(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
            long threadCount
    );

    /**
     * Stop the file system dispatcher.
     *
     * @param pFileSystem The file system object.
     */
    void FspFileSystemStopDispatcher(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem
    );

    void FspDebugLogSetHandle(Pointer pHandle);

    void FspFileSystemSetDebugLogF(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
            @u_int32_t int debugLog
    );

    /**
     * Modify security descriptor.
     * <p>
     * This is a helper for implementing the SetSecurity operation.
     *
     * @param pInputDescriptor        The input security descriptor to be modified.
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
            Pointer /* SECURITY_DESCRIPTOR */ pInputDescriptor,
            @u_int32_t int securityInformation,
            Pointer /* SECURITY_DESCRIPTOR */ pModificationDescriptor,
            @Out PointerByReference /* PSECURITY_DESCRIPTOR */ ppSecurityDescriptor
    );

    /**
     * Add directory information to a buffer.
     * <p>
     * This is a helper for implementing the ReadDirectory operation.
     *
     * @param pDirInfo          The directory information to add. A value of NULL acts as an EOF marker for a ReadDirectory
     *                          operation.
     * @param pBuffer           Pointer to a buffer that will receive the results of the read operation. This should contain
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
    @u_int8_t
    byte FspFileSystemAddDirInfo(
            Pointer /* FSP_FSCTL_DIR_INFO */ pDirInfo,
            Pointer /* VOID */ pBuffer,
            @u_int32_t int length,
            Pointer /* ULONG */ pBytesTransferred
    );

    @FunctionalInterface
    interface GetReparsePointByNameCallback {
        @Delegate
        @u_int32_t
        int GetReparsePointByName(
                Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                Pointer /* VOID */ pContext,
                Pointer /* WSTR */ pFileName,
                byte /* BOOLEAN */ isDirectory,
                Pointer /* VOID */ pBuffer,
                Pointer /* SIZE_T */ pSize
        );
    }

    /**
     * Find reparse point in file name.
     * <p>
     * Given a file name this function returns an index to the first path component that is a reparse
     * point. The function will call the supplied GetReparsePointByName function for every path
     * component until it finds a reparse point or the whole path is processed.
     * <p>
     * This is a helper for implementing the GetSecurityByName operation in file systems
     * that support reparse points.
     *
     * @param pFileSystem           The file system object.
     * @param getReparsePointByName Pointer to function that can retrieve reparse point information by name. The
     *                              FspFileSystemFindReparsePoint will call this function with the Buffer and PSize
     *                              arguments set to NULL. The function should return STATUS_SUCCESS if the passed
     *                              FileName is a reparse point or STATUS_NOT_A_REPARSE_POINT (or other error code)
     *                              otherwise.
     * @param pContext              User context to supply to GetReparsePointByName.
     * @param pFileName             The name of the file or directory.
     * @param pReparsePointIndex    Pointer to a memory location that will receive the index of the first reparse point
     *                              within FileName. A value is only placed in this memory location if the function returns
     *                              TRUE. May be NULL.
     * @return TRUE if a reparse point was found, FALSE otherwise.
     */
    @u_int8_t
    byte FspFileSystemFindReparsePoint(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
            GetReparsePointByNameCallback getReparsePointByName,
            Pointer /* VOID */ pContext,
            Pointer /* WSTR */ pFileName,
            Pointer /* UINT32 */ pReparsePointIndex
    );

    /**
     * Resolve reparse points.
     * <p>
     * Given a file name (and an index where to start resolving) this function will attempt to
     * resolve as many reparse points as possible. The function will call the supplied
     * GetReparsePointByName function for every path component until it resolves the reparse points
     * or the whole path is processed.
     * <p>
     * This is a helper for implementing the ResolveReparsePoints operation in file systems
     * that support reparse points.
     *
     * @param pFileSystem              The file system object.
     * @param getReparsePointByName    Pointer to function that can retrieve reparse point information by name. The function
     *                                 should return STATUS_SUCCESS if the passed FileName is a reparse point or
     *                                 STATUS_NOT_A_REPARSE_POINT (or other error code) otherwise.
     * @param pContext                 User context to supply to GetReparsePointByName.
     * @param pFileName                The name of the file or directory to have its reparse points resolved.
     * @param reparsePointIndex        The index of the first reparse point within FileName.
     * @param resolveLastPathComponent If FALSE, the last path component of FileName should not be resolved, even
     *                                 if it is a reparse point that can be resolved. If TRUE, all path components
     *                                 should be resolved if possible.
     * @param pIoStatus                Pointer to storage that will receive the status to return to the FSD. When
     *                                 this function succeeds it must set PIoStatus->Status to STATUS_REPARSE and
     *                                 PIoStatus->Information to either IO_REPARSE or the reparse tag.
     * @param pBuffer                  Pointer to a buffer that will receive the resolved file name (IO_REPARSE) or
     *                                 reparse data (reparse tag). If the function returns a file name, it should
     *                                 not be NULL terminated.
     * @param pSize                    [in,out]
     *                                 Pointer to the buffer size. On input it contains the size of the buffer.
     *                                 On output it will contain the actual size of data copied.
     * @return STATUS_REPARSE or error code.
     */
    @u_int32_t
    int FspFileSystemResolveReparsePoints(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
            GetReparsePointByNameCallback getReparsePointByName,
            Pointer /* VOID */ pContext,
            Pointer /* WSTR */ pFileName,
            @u_int32_t int reparsePointIndex,
            byte /* BOOLEAN */ resolveLastPathComponent,
            Pointer /* IO_STATUS_BLOCK */ pIoStatus,
            Pointer /* VOID */ pBuffer,
            Pointer /* SIZE_T */ pSize
    );

    /**
     * Test whether reparse data can be replaced.
     * <p>
     * This is a helper for implementing the SetReparsePoint/DeleteReparsePoint operation
     * in file systems that support reparse points.
     *
     * @param pCurrentReparseData    Pointer to the current reparse data.
     * @param currentReparseDataSize Current reparse data size.
     * @param pReplaceReparseData    Pointer to the replacement reparse data.
     * @param replaceReparseDataSize Replacement reparse data size.
     * @return STATUS_SUCCESS or error code.
     */
    @u_int32_t
    int FspFileSystemCanReplaceReparsePoint(
            Pointer /* VOID */ pCurrentReparseData,
            @size_t long currentReparseDataSize,
            Pointer /* VOID */ pReplaceReparseData,
            @size_t long replaceReparseDataSize
    );

    @u_int32_t
    int FspNtStatusFromWin32(@u_int32_t int error);


    @FunctionalInterface
    interface EnumerateEaCallback {
        @Delegate
        @u_int32_t
        int EnumerateEa(
                Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                Pointer /* VOID */ pContext,
                Pointer /* FILE_FULL_EA_INFORMATION */ pSingleEa
        );
    }


    /**
     * Enumerate extended attributes in a buffer.
     * <p>
     * This is a helper for implementing the CreateEx and SetEa operations in file systems
     * that support extended attributes.
     *
     * @param pFileSystem The file system object.
     * @param enumerateEa Pointer to function that receives a single extended attribute. The function
     *                    should return STATUS_SUCCESS or an error code if unsuccessful.
     * @param pContext    User context to supply to EnumEa.
     * @param pEa         Extended attributes buffer.
     * @param eaLength    Extended attributes buffer length.
     * @return STATUS_SUCCESS or error code from EnumerateEa.
     */
    @u_int32_t
    int FspFileSystemEnumerateEa(
            Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
            EnumerateEaCallback enumerateEa,
            Pointer /* VOID */ pContext,
            Pointer /* FILE_FULL_EA_INFORMATION */ pEa,
            @u_int32_t int eaLength
    );

    /**
     * Add extended attribute to a buffer.
     * <p>
     * This is a helper for implementing the GetEa operation.
     *
     * @param pSingleEa         The extended attribute to add. A value of NULL acts as an EOF marker for a GetEa
     *                          operation.
     * @param pEa               Pointer to a buffer that will receive the extended attribute. This should contain
     *                          the same value passed to the GetEa Ea parameter.
     * @param eaLength          Length of buffer. This should contain the same value passed to the GetEa
     *                          EaLength parameter.
     * @param pBytesTransferred [out]
     *                          Pointer to a memory location that will receive the actual number of bytes stored. This should
     *                          contain the same value passed to the GetEa PBytesTransferred parameter.
     * @return TRUE if the extended attribute was added, FALSE if there was not enough space to add it.
     */
    /* BOOLEAN */ byte FspFileSystemAddEa(
            Pointer /* FILE_FULL_EA_INFORMATION */ pSingleEa,
            Pointer /* FILE_FULL_EA_INFORMATION */ pEa,
            @u_int32_t int eaLength,
            Pointer /* ULONG */ pBytesTransferred
    );

    /**
     * Run a service.
     * <p>
     * This function wraps calls to FspServiceCreate, FspServiceLoop and FspServiceDelete to create,
     * run and delete a service. It is intended to be used from a service's main/wmain function.
     * <p>
     * This function runs a service with console mode allowed.
     *
     * @param serviceName  The name of the service.
     * @param onStart      Function to call when the service starts.
     * @param onStop       Function to call when the service stops.
     * @param onControl    Function to call when the service receives a service control code.
     * @param pUserContext Not used
     * @return Service process exit code.
     */
    @u_int32_t
    int FspServiceRunEx(byte[] serviceName,
                        FspServiceStartCallback onStart,
                        FspServiceStopCallback onStop,
                        FspServiceControlCallback onControl,
                        Pointer /* VOID */ pUserContext
    );

    @FunctionalInterface
    interface FspServiceStartCallback {
        /**
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int FspServiceStart(Pointer /* FSP_SERVICE */ pService,
                            int argc,
                            Pointer /* PWSTR */ argv
        );
    }

    @FunctionalInterface
    interface FspServiceStopCallback {
        /**
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int FspServiceStop(Pointer /* FSP_SERVICE */ pService);
    }

    @FunctionalInterface
    interface FspServiceControlCallback {
        /**
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int FspServiceControl(Pointer /* FSP_SERVICE */ pService,
                              int control,
                              int eventType,
                              Pointer /* VOID */ pEventData);
    }
}
