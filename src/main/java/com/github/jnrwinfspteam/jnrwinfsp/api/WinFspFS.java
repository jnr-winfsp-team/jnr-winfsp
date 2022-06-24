package com.github.jnrwinfspteam.jnrwinfsp.api;

import jnr.ffi.Pointer;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface WinFspFS {
    /**
     * Get volume information.
     */
    VolumeInfo getVolumeInfo() throws NTStatusException;

    /**
     * Set volume label.
     *
     * @param volumeLabel The new label for the volume.
     */
    VolumeInfo setVolumeLabel(String volumeLabel) throws NTStatusException;

    /**
     * Get file or directory security descriptor string, and file attributes.
     * If the file or directory does not exist, then an empty optional must be returned.
     *
     * @param fileName The name of the file or directory to get the security descriptor and attributes for.
     */
    Optional<SecurityResult> getSecurityByName(String fileName) throws NTStatusException;

    /**
     * Create new file or directory.
     *
     * @param fileName           The name of the file or directory to be created.
     * @param createOptions      Create options for this request. This parameter has the same meaning as the
     *                           CreateOptions parameter of the NtCreateFile API. User mode file systems should typically
     *                           only be concerned with the flag FILE_DIRECTORY_FILE, which is an instruction to create a
     *                           directory rather than a file. Some file systems may also want to pay attention to the
     *                           FILE_NO_INTERMEDIATE_BUFFERING and FILE_WRITE_THROUGH flags, although these are
     *                           typically handled by the FSD component.
     * @param grantedAccess      Determines the specific access rights that have been granted for this request. Upon
     *                           receiving this call all access checks have been performed and the user mode file system
     *                           need not perform any additional checks. However this parameter may be useful to a user
     *                           mode file system; for example the WinFsp-FUSE layer uses this parameter to determine
     *                           which flags to use in its POSIX open() call.
     * @param fileAttributes     File attributes to apply to the newly created file or directory.
     * @param securityDescriptor Security descriptor to apply to the newly created file or directory. This security
     *                           descriptor will always be in self-relative format. Will be NULL for named streams.
     * @param allocationSize     Allocation size for the newly created file.
     * @param reparsePoint       (optional) Reparse point
     */
    FileInfo create(String fileName,
                    Set<CreateOptions> createOptions,
                    int grantedAccess,
                    Set<FileAttributes> fileAttributes,
                    byte[] securityDescriptor,
                    long allocationSize,
                    ReparsePoint reparsePoint
    ) throws NTStatusException;

    /**
     * Open a file or directory.
     *
     * @param fileName      The name of the file or directory to be opened.
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
     */
    FileInfo open(String fileName, Set<CreateOptions> createOptions, int grantedAccess) throws NTStatusException;

    /**
     * Overwrite a file.
     *
     * @param fileName              The name of the file being overwritten
     * @param fileAttributes        File attributes to apply to the overwritten file.
     * @param replaceFileAttributes When TRUE the existing file attributes should be replaced with the new ones.
     *                              When FALSE the existing file attributes should be merged (or'ed) with the new ones.
     * @param allocationSize        Allocation size for the overwritten file.
     */
    FileInfo overwrite(String fileName,
                       Set<FileAttributes> fileAttributes,
                       boolean replaceFileAttributes,
                       long allocationSize
    ) throws NTStatusException;

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
     * @param ctx   The context of the file or directory to clean up.
     * @param flags These flags determine whether the file was modified and whether to delete the file.
     */
    void cleanup(OpenContext ctx, Set<CleanupFlags> flags);

    /**
     * Close a file.
     *
     * @param ctx The context of the file or directory to be closed.
     */
    void close(OpenContext ctx);

    /**
     * Read a file.
     * <p>
     * NOTE: STATUS_PENDING is supported allowing for asynchronous operation.
     *
     * @param fileName The name of the file to be read.
     * @param pBuffer  Pointer to a buffer that will receive the results of the read operation.
     * @param offset   Offset within the file to read from.
     * @param length   Length of data to read.
     */
    long read(String fileName, Pointer pBuffer, long offset, int length) throws NTStatusException;

    /**
     * Write a file.
     * <p>
     * NOTE: STATUS_PENDING is supported allowing for asynchronous operation.
     *
     * @param fileName         The name of the file to be written.
     * @param pBuffer          Pointer to a buffer that contains the data to write.
     * @param offset           Offset within the file to write to.
     * @param length           Length of data to write.
     * @param writeToEndOfFile When TRUE the file system must write to the current end of file. In this case the Offset
     *                         parameter will contain the value -1.
     * @param constrainedIo    When TRUE the file system must not extend the file (i.e. change the file size).
     */
    WriteResult write(String fileName,
                      Pointer pBuffer,
                      long offset,
                      int length,
                      boolean writeToEndOfFile,
                      boolean constrainedIo
    ) throws NTStatusException;

    /**
     * Flush a file or volume.
     * <p>
     * Note that the FSD will also flush all file/volume caches prior to invoking this operation.
     *
     * @param fileName The name of the file to be flushed. When NULL the whole volume is being flushed.
     */
    FileInfo flush(String fileName) throws NTStatusException;

    /**
     * Get file or directory information.
     *
     * @param ctx The context of the file or directory to get information for.
     */
    FileInfo getFileInfo(OpenContext ctx) throws NTStatusException;

    /**
     * Set file or directory basic information.
     *
     * @param ctx            The context of the file or directory to set information for.
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
     */
    FileInfo setBasicInfo(OpenContext ctx,
                          Set<FileAttributes> fileAttributes,
                          WinSysTime creationTime,
                          WinSysTime lastAccessTime,
                          WinSysTime lastWriteTime,
                          WinSysTime changeTime
    ) throws NTStatusException;

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
     * @param fileName          The name of the file to set the file/allocation size for.
     * @param newSize           New file/allocation size to apply to the file.
     * @param setAllocationSize If TRUE, then the allocation size is being set. if FALSE, then the file size is being set.
     */
    FileInfo setFileSize(String fileName, long newSize, boolean setAllocationSize) throws NTStatusException;

    /**
     * Determine whether a file or directory can be deleted.
     * <p>
     * This function tests whether a file or directory can be safely deleted. This function does
     * not need to perform access checks, but may perform tasks such as check for empty
     * directories, etc.
     * <p>
     * This function should <b>NEVER</b> delete the file or directory in question. Deletion should
     * happen during Cleanup with the FspCleanupDelete flag set.
     * <p>
     * This function gets called when Win32 APIs such as DeleteFile or RemoveDirectory are used.
     * It does not get called when a file or directory is opened with FILE_DELETE_ON_CLOSE.
     * <p>
     * NOTE: If both CanDelete and SetDelete are defined, SetDelete takes precedence. However
     * most file systems need only implement the CanDelete operation.
     *
     * @param ctx The context of the file or directory to test for deletion.
     */
    void canDelete(OpenContext ctx) throws NTStatusException;

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
     * @param ctx             The context of the file or directory to rename.
     * @param newFileName     The new name for the file or directory.
     * @param replaceIfExists Whether to replace a file that already exists at NewFileName.
     */
    void rename(OpenContext ctx, String newFileName, boolean replaceIfExists) throws NTStatusException;

    /**
     * Get file or directory security descriptor.
     *
     * @param ctx The context of the file or directory to get the security descriptor for.
     */
    byte[] getSecurity(OpenContext ctx) throws NTStatusException;

    /**
     * Set file or directory security descriptor string.
     *
     * @param ctx                The context of the file or directory to set the security descriptor for.
     * @param securityDescriptor A security descriptor
     */
    void setSecurity(OpenContext ctx, byte[] securityDescriptor) throws NTStatusException;

    /**
     * Reads a directory. Each directory entry is passed to the given consumer.
     * This method may be called multiple times for a given directory, each time with a different marker.
     *
     * @param dirName  The name of the directory to be read.
     * @param pattern  The pattern to match against files in this directory. Can be NULL. The file system
     *                 can choose to ignore this parameter as the FSD will always perform its own pattern
     *                 matching on the returned results.
     * @param marker   A file name that marks where in the directory to start reading. Files with names
     *                 that are greater than (not equal to) this marker (in the directory order determined
     *                 by the file system) should be returned. Can be NULL.
     * @param consumer A consumer that accepts directory entries, one by one. Will return true while more
     *                 entries can be added, and false when no more can be added due to lack of memory.
     */
    void readDirectory(String dirName, String pattern, String marker, Predicate<FileInfo> consumer)
            throws NTStatusException;

    /**
     * Get directory information for a single file or directory within a parent directory.
     *
     * @param parentDirName The name of the parent directory.
     * @param fileName      The name of the file or directory to get information for. This name is relative
     *                      to the parent directory and is a single path component.
     */
    FileInfo getDirInfoByName(String parentDirName, String fileName) throws NTStatusException;

    /**
     * Get reparse point data.
     *
     * @param ctx The context of the file or directory to be read.
     */
    byte[] getReparsePointData(OpenContext ctx) throws NTStatusException;

    /**
     * Sets a reparse point.
     *
     * @param ctx         The context of the file or directory to be read
     * @param reparseData The reparse point data
     * @param reparseTag  The reparse point tag
     */
    void setReparsePoint(OpenContext ctx, byte[] reparseData, int reparseTag) throws NTStatusException;

    /**
     * Deletes a reparse point.
     *
     * @param ctx The context of the file or directory to be read
     */
    void deleteReparsePoint(OpenContext ctx) throws NTStatusException;
}
