package com.github.jnrwinfspteam.jnrwinfsp.lib;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.types.size_t;
import jnr.ffi.types.u_int32_t;
import jnr.ffi.types.u_int64_t;

public final class WinFspCallbacks {

    @FunctionalInterface
    public interface GetVolumeInfoCallback {
        /**
         * Get volume information.
         *
         * @param pFileSystem The file system on which this request is posted.
         * @param pVolumeInfo [out]
         *                    Pointer to a structure that will receive the volume information on successful return
         *                    from this call.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int GetVolumeInfo(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                          Pointer /* FSP_FSCTL_VOLUME_INFO */ pVolumeInfo
        );
    }

    @FunctionalInterface
    public interface SetVolumeLabelCallback {
        /**
         * Set volume label.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pVolumeLabel The new label for the volume.
         * @param pVolumeInfo  [out]
         *                     Pointer to a structure that will receive the volume information on successful return
         *                     from this call.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int SetVolumeLabel(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                           Pointer /* WSTR */ pVolumeLabel,
                           Pointer /* FSP_FSCTL_VOLUME_INFO */ pVolumeInfo
        );
    }

    @FunctionalInterface
    public interface GetSecurityByNameCallback {
        /**
         * Get file or directory attributes and security descriptor given a file name.
         *
         * @param pFileSystem             The file system on which this request is posted.
         * @param pFileName               The name of the file or directory to get the attributes and security descriptor for.
         * @param pFileAttributes         Pointer to a memory location that will receive the file attributes on successful return
         *                                from this call. May be NULL.
         *                                <p>
         *                                If this call returns STATUS_REPARSE, the file system MAY place here the index of the
         *                                first reparse point within FileName. The file system MAY also leave this at its default
         *                                value of 0.
         * @param pSecurityDescriptor     Pointer to a buffer that will receive the file security descriptor on successful return
         *                                from this call. May be NULL.
         * @param pSecurityDescriptorSize [in,out]
         *                                Pointer to the security descriptor buffer size. On input it contains the size of the
         *                                security descriptor buffer. On output it will contain the actual size of the security
         *                                descriptor copied into the security descriptor buffer. May be NULL.
         * @return STATUS_SUCCESS, STATUS_REPARSE or error code.
         * <p>
         * STATUS_REPARSE should be returned by file systems that support reparse points when
         * they encounter a FileName that contains reparse points anywhere but the final path
         * component.
         */
        @Delegate
        @u_int32_t
        int GetSecurityByName(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                              Pointer /* WSTR */ pFileName,
                              Pointer /* UINT32 */ pFileAttributes /* or ReparsePointIndex */,
                              Pointer /* SECURITY_DESCRIPTOR */ pSecurityDescriptor,
                              Pointer /* SIZE_T */ pSecurityDescriptorSize
        );
    }

    @FunctionalInterface
    public interface CreateExCallback {
        /**
         * Create new file or directory.
         * <p>
         * This function works like Create, except that it also accepts an extra buffer that
         * may contain extended attributes or a reparse point.
         * <p>
         * NOTE: If both Create and CreateEx are defined, CreateEx takes precedence.
         *
         * @param pFileSystem               The file system on which this request is posted.
         * @param pFileName                 The name of the file or directory to be created.
         * @param createOptions             Create options for this request. This parameter has the same meaning as the
         *                                  CreateOptions parameter of the NtCreateFile API. User mode file systems should typically
         *                                  only be concerned with the flag FILE_DIRECTORY_FILE, which is an instruction to create a
         *                                  directory rather than a file. Some file systems may also want to pay attention to the
         *                                  FILE_NO_INTERMEDIATE_BUFFERING and FILE_WRITE_THROUGH flags, although these are
         *                                  typically handled by the FSD component.
         * @param grantedAccess             Determines the specific access rights that have been granted for this request. Upon
         *                                  receiving this call all access checks have been performed and the user mode file system
         *                                  need not perform any additional checks. However this parameter may be useful to a user
         *                                  mode file system; for example the WinFsp-FUSE layer uses this parameter to determine
         *                                  which flags to use in its POSIX open() call.
         * @param fileAttributes            File attributes to apply to the newly created file or directory.
         * @param pSecurityDescriptor       Security descriptor to apply to the newly created file or directory. This security
         *                                  descriptor will always be in self-relative format. Its length can be retrieved using the
         *                                  Windows GetSecurityDescriptorLength API. Will be NULL for named streams.
         * @param allocationSize            Allocation size for the newly created file.
         * @param pExtraBuffer              Extended attributes or reparse point buffer.
         * @param extraLength               Extended attributes or reparse point buffer length.
         * @param extraBufferIsReparsePoint FALSE: extra buffer is extended attributes; TRUE: extra buffer is reparse point.
         * @param ppFileContext             [out]
         *                                  Pointer that will receive the file context on successful return from this call.
         * @param pFileInfo                 [out]
         *                                  Pointer to a structure that will receive the file information on successful return
         *                                  from this call. This information includes file attributes, file times, etc.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int CreateEx(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                     Pointer /* WSTR */ pFileName,
                     @u_int32_t int createOptions,
                     @u_int32_t int grantedAccess,
                     @u_int32_t int fileAttributes,
                     Pointer /* SECURITY_DESCRIPTOR */ pSecurityDescriptor,
                     @u_int64_t long allocationSize,
                     Pointer /* VOID */ pExtraBuffer,
                     long extraLength,
                     byte /* BOOLEAN */ extraBufferIsReparsePoint,
                     Pointer /* PVOID */ ppFileContext,
                     Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface OpenCallback {
        /**
         * Open a file or directory.
         *
         * @param pFileSystem   The file system on which this request is posted.
         * @param pFileName     The name of the file or directory to be opened.
         * @param createOptions Create options for this request. This parameter has the same meaning as the
         *                      CreateOptions parameter of the NtCreateFile API. User mode file systems typically
         *                      do not need to do anything special with respect to this parameter. Some file systems may
         *                      also want to pay attention to the FILE_NO_INTERMEDIATE_BUFFERING and FILE_WRITE_THROUGH
         *                      flags, although these are typically handled by the FSD component.
         * @param grantedAccess Determines the specific access rights that have been granted for this request. Upon
         *                      receiving this call all access checks have been performed and the user mode file system
         *                      need not perform any additional checks. However this parameter may be useful to a user
         *                      mode file system; for example the WinFsp-FUSE layer uses this parameter to determine
         *                      which flags to use in its POSIX open() call.
         * @param ppFileContext [out]
         *                      Pointer that will receive the file context on successful return from this call.
         * @param pFileInfo     [out]
         *                      Pointer to a structure that will receive the file information on successful return
         *                      from this call. This information includes file attributes, file times, etc.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int Open(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                 Pointer /* WSTR */ pFileName,
                 @u_int32_t int createOptions,
                 @u_int32_t int grantedAccess,
                 Pointer /* PVOID */ ppFileContext,
                 Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface OverwriteCallback {
        /**
         * Overwrite a file.
         *
         * @param pFileSystem           The file system on which this request is posted.
         * @param pFileContext          The file context of the file to overwrite.
         * @param fileAttributes        File attributes to apply to the overwritten file.
         * @param replaceFileAttributes When TRUE the existing file attributes should be replaced with the new ones.
         *                              When FALSE the existing file attributes should be merged (or'ed) with the new ones.
         * @param allocationSize        Allocation size for the overwritten file.
         * @param pFileInfo             [out]
         *                              Pointer to a structure that will receive the file information on successful return
         *                              from this call. This information includes file attributes, file times, etc.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int Overwrite(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                      Pointer /* VOID */ pFileContext,
                      @u_int32_t int fileAttributes,
                      byte /* BOOLEAN */ replaceFileAttributes,
                      @u_int64_t long allocationSize,
                      Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface CleanupCallback {
        /**
         * Cleanup a file.
         * <p>
         * When CreateFile is used to open or create a file the kernel creates a kernel mode file
         * object (type FILE_OBJECT) and a handle for it, which it returns to user-mode. The handle may
         * be duplicated (using DuplicateHandle), but all duplicate handles always refer to the same
         * file object. When all handles for a particular file object get closed (using CloseHandle)
         * the system sends a Cleanup request to the file system.
         * <p>
         * There will be a Cleanup operation for every Create or Open operation posted to the user mode
         * file system. However the Cleanup operation is <b>not</b> the final close operation on a file.
         * The file system must be ready to receive additional operations until close time. This is true
         * even when the file is being deleted!
         * <p>
         * The Flags parameter contains information about the cleanup operation:
         * <ul>
         * <li>FspCleanupDelete -
         * An important function of the Cleanup operation is to complete a delete operation. Deleting
         * a file or directory in Windows is a three-stage process where the file is first opened, then
         * tested to see if the delete can proceed and if the answer is positive the file is then
         * deleted during Cleanup.
         * <p>
         * When this flag is set, this is the last outstanding cleanup for this particular file node.
         * </li>
         * <li>FspCleanupSetAllocationSize -
         * The NTFS and FAT file systems reset a file's allocation size when they receive the last
         * outstanding cleanup for a particular file node. User mode file systems that implement
         * allocation size and wish to duplicate the NTFS and FAT behavior can use this flag.
         * </li>
         * <li>
         * FspCleanupSetArchiveBit -
         * File systems that support the archive bit should set the file node's archive bit when this
         * flag is set.
         * </li>
         * <li>FspCleanupSetLastAccessTime, FspCleanupSetLastWriteTime, FspCleanupSetChangeTime - File
         * systems should set the corresponding file time when each one of these flags is set. Note that
         * updating the last access time is expensive and a file system may choose to not implement it.
         * </ul>
         * <p>
         * There is no way to report failure of this operation. This is a Windows limitation.
         * <p>
         * As an optimization a file system may specify the FSP_FSCTL_VOLUME_PARAMS ::
         * PostCleanupWhenModifiedOnly flag. In this case the FSD will only post Cleanup requests when
         * the file was modified/deleted.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the file or directory to cleanup.
         * @param pFileName    The name of the file or directory to cleanup. Sent only when a Delete is requested.
         * @param flags        These flags determine whether the file was modified and whether to delete the file.
         *                     <p>
         *                     see
         *                     Close
         *                     CanDelete
         *                     SetDelete
         */
        @Delegate
        void Cleanup(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                     Pointer /* VOID */ pFileContext,
                     Pointer /* WSTR */ pFileName,
                     @u_int32_t int flags
        );
    }

    @FunctionalInterface
    public interface CloseCallback {
        /**
         * Close a file.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the file or directory to be closed.
         */
        @Delegate
        void Close(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                   Pointer /* VOID */ pFileContext
        );
    }

    @FunctionalInterface
    public interface ReadCallback {
        /**
         * Read a file.
         *
         * @param pFileSystem       The file system on which this request is posted.
         * @param pFileContext      The file context of the file to be read.
         * @param pBuffer           Pointer to a buffer that will receive the results of the read operation.
         * @param offset            Offset within the file to read from.
         * @param length            Length of data to read.
         * @param pBytesTransferred [out]
         *                          Pointer to a memory location that will receive the actual number of bytes read.
         * @return STATUS_SUCCESS or error code. STATUS_PENDING is supported allowing for asynchronous
         * operation.
         */
        @Delegate
        @u_int32_t
        int Read(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                 Pointer /* VOID */ pFileContext,
                 Pointer /* VOID */ pBuffer,
                 @u_int64_t long offset,
                 @u_int32_t int length,
                 Pointer /* ULONG */ pBytesTransferred
        );
    }

    @FunctionalInterface
    public interface WriteCallback {
        /**
         * Write a file.
         *
         * @param pFileSystem       The file system on which this request is posted.
         * @param pFileContext      The file context of the file to be written.
         * @param pBuffer           Pointer to a buffer that contains the data to write.
         * @param offset            Offset within the file to write to.
         * @param length            Length of data to write.
         * @param writeToEndOfFile  When TRUE the file system must write to the current end of file. In this case the Offset
         *                          parameter will contain the value -1.
         * @param constrainedIo     When TRUE the file system must not extend the file (i.e. change the file size).
         * @param pBytesTransferred [out]
         *                          Pointer to a memory location that will receive the actual number of bytes written.
         * @param pFileInfo         [out]
         *                          Pointer to a structure that will receive the file information on successful return
         *                          from this call. This information includes file attributes, file times, etc.
         * @return STATUS_SUCCESS or error code. STATUS_PENDING is supported allowing for asynchronous
         * operation.
         */
        @Delegate
        @u_int32_t
        int Write(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                  Pointer /* VOID */ pFileContext,
                  Pointer /* VOID */ pBuffer,
                  @u_int64_t long offset,
                  @u_int32_t int length,
                  byte /* BOOLEAN */ writeToEndOfFile,
                  byte /* BOOLEAN */ constrainedIo,
                  Pointer /* ULONG */ pBytesTransferred,
                  Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface FlushCallback {
        /**
         * Flush a file or volume.
         * <p>
         * Note that the FSD will also flush all file/volume caches prior to invoking this operation.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the file to be flushed. When NULL the whole volume is being flushed.
         * @param pFileInfo    [out]
         *                     Pointer to a structure that will receive the file information on successful return
         *                     from this call. This information includes file attributes, file times, etc. Used when
         *                     flushing file (not volume).
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int Flush(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                  Pointer /* VOID */ pFileContext,
                  Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface GetFileInfoCallback {
        /**
         * Get file or directory information.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the file or directory to get information for.
         * @param pFileInfo    [out]
         *                     Pointer to a structure that will receive the file information on successful return
         *                     from this call. This information includes file attributes, file times, etc.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int GetFileInfo(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                        Pointer /* VOID */ pFileContext,
                        Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface SetBasicInfoCallback {
        /**
         * Set file or directory basic information.
         *
         * @param pFileSystem    The file system on which this request is posted.
         * @param pFileContext   The file context of the file or directory to set information for.
         * @param fileAttributes File attributes to apply to the file or directory. If the value INVALID_FILE_ATTRIBUTES
         *                       is sent, the file attributes should not be changed.
         * @param creationTime   Creation time to apply to the file or directory. If the value 0 is sent, the creation
         *                       time should not be changed.
         * @param lastAccessTime Last access time to apply to the file or directory. If the value 0 is sent, the last
         *                       access time should not be changed.
         * @param lastWriteTime  Last write time to apply to the file or directory. If the value 0 is sent, the last
         *                       write time should not be changed.
         * @param changeTime     Change time to apply to the file or directory. If the value 0 is sent, the change time
         *                       should not be changed.
         * @param pFileInfo      [out]
         *                       Pointer to a structure that will receive the file information on successful return
         *                       from this call. This information includes file attributes, file times, etc.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int SetBasicInfo(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                         Pointer /* VOID */ pFileContext,
                         @u_int32_t int fileAttributes,
                         @u_int64_t long creationTime,
                         @u_int64_t long lastAccessTime,
                         @u_int64_t long lastWriteTime,
                         @u_int64_t long changeTime,
                         Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface SetFileSizeCallback {
        /**
         * Set file/allocation size.
         * <p>
         * This function is used to change a file's sizes. Windows file systems maintain two kinds
         * of sizes: the file size is where the End Of File (EOF) is, and the allocation size is the
         * actual size that a file takes up on the "disk".
         * <p>
         * The rules regarding file/allocation size are:
         * <ul>
         * <li>Allocation size must always be aligned to the allocation unit boundary. The allocation
         * unit is the product <code>(UINT64)SectorSize * (UINT64)SectorsPerAllocationUnit</code> from
         * the FSP_FSCTL_VOLUME_PARAMS structure. The FSD will always send properly aligned allocation
         * sizes when setting the allocation size.</li>
         * <li>Allocation size is always greater or equal to the file size.</li>
         * <li>A file size of more than the current allocation size will also extend the allocation
         * size to the next allocation unit boundary.</li>
         * <li>An allocation size of less than the current file size should also truncate the current
         * file size.</li>
         * </ul>
         *
         * @param pFileSystem       The file system on which this request is posted.
         * @param pFileContext      The file context of the file to set the file/allocation size for.
         * @param newSize           New file/allocation size to apply to the file.
         * @param setAllocationSize If TRUE, then the allocation size is being set. if FALSE, then the file size is being set.
         * @param pFileInfo         [out]
         *                          Pointer to a structure that will receive the file information on successful return
         *                          from this call. This information includes file attributes, file times, etc.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int SetFileSize(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                        Pointer /* VOID */ pFileContext,
                        @u_int64_t long newSize,
                        byte /* BOOLEAN */ setAllocationSize,
                        Pointer /* FSP_FSCTL_FILE_INFO */ pFileInfo
        );
    }

    @FunctionalInterface
    public interface CanDeleteCallback {
        /**
         * Determine whether a file or directory can be deleted.
         * <p>
         * This function tests whether a file or directory can be safely deleted. This function does
         * not need to perform access checks, but may performs tasks such as check for empty
         * directories, etc.
         * <p>
         * This function should <b>NEVER</b> delete the file or directory in question. Deletion should
         * happen during Cleanup with the FspCleanupDelete flag set.
         * <p>
         * This function gets called when Win32 API's such as DeleteFile or RemoveDirectory are used.
         * It does not get called when a file or directory is opened with FILE_DELETE_ON_CLOSE.
         * <p>
         * NOTE: If both CanDelete and SetDelete are defined, SetDelete takes precedence. However
         * most file systems need only implement the CanDelete operation.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the file or directory to test for deletion.
         * @param pFileName    The name of the file or directory to test for deletion.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int CanDelete(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                      Pointer /* VOID */ pFileContext,
                      Pointer /* WSTR */ pFileName
        );
    }

    @FunctionalInterface
    public interface RenameCallback {
        /**
         * Renames a file or directory.
         * <p>
         * The kernel mode FSD provides certain guarantees prior to posting a rename operation:
         * <ul>
         * <li>A file cannot be renamed if a file with the same name exists and has open handles.</li>
         * <li>A directory cannot be renamed if it or any of its subdirectories contains a file that
         * has open handles.</li>
         * </ul>
         *
         * @param pFileSystem     The file system on which this request is posted.
         * @param pFileContext    The file context of the file or directory to be renamed.
         * @param pFileName       The current name of the file or directory to rename.
         * @param pNewFileName    The new name for the file or directory.
         * @param replaceIfExists Whether to replace a file that already exists at NewFileName.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int Rename(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                   Pointer /* VOID */ pFileContext,
                   Pointer /* WSTR */ pFileName,
                   Pointer /* WSTR */ pNewFileName,
                   byte /* BOOLEAN */ replaceIfExists
        );
    }

    @FunctionalInterface
    public interface GetSecurityCallback {
        /**
         * Get file or directory security descriptor.
         *
         * @param pFileSystem             The file system on which this request is posted.
         * @param pFileContext            The file context of the file or directory to get the security descriptor for.
         * @param pSecurityDescriptor     Pointer to a buffer that will receive the file security descriptor on successful return
         *                                from this call. May be NULL.
         * @param pSecurityDescriptorSize [in,out]
         *                                Pointer to the security descriptor buffer size. On input it contains the size of the
         *                                security descriptor buffer. On output it will contain the actual size of the security
         *                                descriptor copied into the security descriptor buffer. Cannot be NULL.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int GetSecurity(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                        Pointer /* VOID */ pFileContext,
                        Pointer /* SECURITY_DESCRIPTOR */ pSecurityDescriptor,
                        Pointer /* SIZE_T */ pSecurityDescriptorSize
        );
    }

    @FunctionalInterface
    public interface SetSecurityCallback {
        /**
         * Set file or directory security descriptor. See FspSetSecurityDescriptor or FspDeleteSecurityDescriptor
         * for more details.
         *
         * @param pFileSystem             The file system on which this request is posted.
         * @param pFileContext            The file context of the file or directory to set the security descriptor for.
         * @param securityInformation     Describes what parts of the file or directory security descriptor should
         *                                be modified.
         * @param pModificationDescriptor Describes the modifications to apply to the file or directory security descriptor.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int SetSecurity(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                        Pointer /* VOID */ pFileContext,
                        @u_int32_t int securityInformation,
                        Pointer /* SECURITY_DESCRIPTOR */ pModificationDescriptor
        );
    }

    @FunctionalInterface
    public interface ReadDirectoryCallback {
        /**
         * Read a directory.
         *
         * @param pFileSystem       The file system on which this request is posted.
         * @param pFileContext      The file context of the directory to be read.
         * @param pPattern          The pattern to match against files in this directory. Can be NULL. The file system
         *                          can choose to ignore this parameter as the FSD will always perform its own pattern
         *                          matching on the returned results.
         * @param pMarker           A file name that marks where in the directory to start reading. Files with names
         *                          that are greater than (not equal to) this marker (in the directory order determined
         *                          by the file system) should be returned. Can be NULL.
         * @param pBuffer           Pointer to a buffer that will receive the results of the read operation.
         * @param length            Length of data to read.
         * @param pBytesTransferred [out]
         *                          Pointer to a memory location that will receive the actual number of bytes read.
         * @return STATUS_SUCCESS or error code. STATUS_PENDING is supported allowing for asynchronous
         * operation.
         * <p>
         * see FspFileSystemAddDirInfo
         */
        @Delegate
        @u_int32_t
        int ReadDirectory(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                          Pointer /* VOID */ pFileContext,
                          Pointer /* WSTR */ pPattern,
                          Pointer /* WSTR */ pMarker,
                          Pointer /* VOID */ pBuffer,
                          @u_int32_t int length,
                          Pointer /* ULONG */ pBytesTransferred
        );
    }

    @FunctionalInterface
    public interface ResolveReparsePointsCallback {
        /**
         * Resolve reparse points.
         * <p>
         * Reparse points are a general mechanism for attaching special behavior to files.
         * A file or directory can contain a reparse point. A reparse point is data that has
         * special meaning to the file system, Windows or user applications. For example, NTFS
         * and Windows use reparse points to implement symbolic links. As another example,
         * a particular file system may use reparse points to emulate UNIX FIFO's.
         * <p>
         * This function is expected to resolve as many reparse points as possible. If a reparse
         * point is encountered that is not understood by the file system further reparse point
         * resolution should stop; the reparse point data should be returned to the FSD with status
         * STATUS_REPARSE/reparse-tag. If a reparse point (symbolic link) is encountered that is
         * understood by the file system but points outside it, the reparse point should be
         * resolved, but further reparse point resolution should stop; the resolved file name
         * should be returned to the FSD with status STATUS_REPARSE/IO_REPARSE.
         *
         * @param pFileSystem              The file system on which this request is posted.
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
        @Delegate
        @u_int32_t
        int ResolveReparsePoints(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                                 Pointer /* WSTR */ pFileName,
                                 @u_int32_t int reparsePointIndex,
                                 byte /* BOOLEAN */ resolveLastPathComponent,
                                 Pointer /* IO_STATUS_BLOCK */ pIoStatus,
                                 Pointer /* VOID */ pBuffer,
                                 Pointer /* SIZE_T */ pSize
        );
    }

    @FunctionalInterface
    public interface GetReparsePointCallback {
        /**
         * Get reparse point.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the reparse point.
         * @param pFileName    The file name of the reparse point.
         * @param pBuffer      Pointer to a buffer that will receive the results of this operation. If
         *                     the function returns a symbolic link path, it should not be NULL terminated.
         * @param pSize        [in,out]
         *                     Pointer to the buffer size. On input it contains the size of the buffer.
         *                     On output it will contain the actual size of data copied.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int GetReparsePoint(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                            Pointer /* VOID */ pFileContext,
                            Pointer /* WSTR */ pFileName,
                            Pointer /* VOID */ pBuffer,
                            Pointer /* SIZE_T */ pSize
        );
    }

    @FunctionalInterface
    public interface SetReparsePointCallback {
        /**
         * Set reparse point.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the reparse point.
         * @param pFileName    The file name of the reparse point.
         * @param pBuffer      Pointer to a buffer that contains the data for this operation. If this buffer
         *                     contains a symbolic link path, it should not be assumed to be NULL terminated.
         * @param size         Size of data to write.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int SetReparsePoint(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                            Pointer /* VOID */ pFileContext,
                            Pointer /* WSTR */ pFileName,
                            Pointer /* VOID */ pBuffer,
                            @size_t long size
        );
    }

    @FunctionalInterface
    public interface DeleteReparsePointCallback {
        /**
         * Delete reparse point.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the reparse point.
         * @param pFileName    The file name of the reparse point.
         * @param pBuffer      Pointer to a buffer that contains the data for this operation.
         * @param size         Size of data to write.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int DeleteReparsePoint(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                               Pointer /* VOID */ pFileContext,
                               Pointer /* WSTR */ pFileName,
                               Pointer /* VOID */ pBuffer,
                               @size_t long size
        );
    }

    @FunctionalInterface
    public interface GetDirInfoByNameCallback {
        /**
         * Get directory information for a single file or directory within a parent directory.
         *
         * @param pFileSystem  The file system on which this request is posted.
         * @param pFileContext The file context of the parent directory.
         * @param pFileName    The name of the file or directory to get information for. This name is relative
         *                     to the parent directory and is a single path component.
         * @param pDirInfo     [out]
         *                     Pointer to a structure that will receive the directory information on successful
         *                     return from this call. This information includes the file name, but also file
         *                     attributes, file times, etc.
         * @return STATUS_SUCCESS or error code.
         */
        @Delegate
        @u_int32_t
        int GetDirInfoByName(Pointer /* FSP_FILE_SYSTEM */ pFileSystem,
                             Pointer /* VOID */ pFileContext,
                             Pointer /* WSTR */ pFileName,
                             Pointer /* FSP_FSCTL_DIR_INFO */ pDirInfo);
    }

    private WinFspCallbacks() {
        // not instantiable
    }
}