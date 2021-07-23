package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.flags.CleanupFlags;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;
import jnr.ffi.Pointer;

import java.util.List;
import java.util.Set;

public interface WinFspFS extends Mountable {
    /**
     * Get volume information.
     *
     * @param fileSystem The file system on which this request is posted.
     */
    VolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) throws NTStatusException;

    /**
     * Set volume label.
     *
     * @param fileSystem  The file system on which this request is posted.
     * @param volumeLabel The new label for the volume.
     */
    VolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) throws NTStatusException;

    /**
     * Get file or directory attributes and security descriptor given a file name.
     *
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file or directory to get the attributes and security descriptor for.
     * @return result with:
     * <ul>
     *     <li>STATUS_SUCCESS(0) and (non-null) file attributes, security descriptor and size</li>
     *     <li>OR STATUS_REPARSE(0x104) and index of the first reparse point within fileName (may be 0), security descriptor and size </li>
     *     <li>OR error code and null file attributes, security descriptor and size</li>
     * </ul>
     * <p>
     *  NOTE: STATUS_REPARSE(0x104) should be returned by file systems that support reparse points when
     *  they encounter a fileName that contains reparse points anywhere but the final path
     *  component.
     */
    ResultSecurityAndAttributes getSecurityByName(FSP_FILE_SYSTEM fileSystem, String fileName);

    /**
     * Create new file or directory.
     *
     * @param fileSystem          The file system on which this request is posted.
     * @param fileName            The name of the file or directory to be created.
     * @param createOptions       Create options for this request. This parameter has the same meaning as the
     *                            CreateOptions parameter of the NtCreateFile API. User mode file systems should typically
     *                            only be concerned with the flag FILE_DIRECTORY_FILE, which is an instruction to create a
     *                            directory rather than a file. Some file systems may also want to pay attention to the
     *                            FILE_NO_INTERMEDIATE_BUFFERING and FILE_WRITE_THROUGH flags, although these are
     *                            typically handled by the FSD component.
     * @param grantedAccess       Determines the specific access rights that have been granted for this request. Upon
     *                            receiving this call all access checks have been performed and the user mode file system
     *                            need not perform any additional checks. However this parameter may be useful to a user
     *                            mode file system; for example the WinFsp-FUSE layer uses this parameter to determine
     *                            which flags to use in its POSIX open() call.
     * @param fileAttributes      File attributes to apply to the newly created file or directory.
     * @param pSecurityDescriptor Security descriptor to apply to the newly created file or directory. This security
     *                            descriptor will always be in self-relative format. Its length can be retrieved using the
     *                            Windows GetSecurityDescriptorLength API. Will be NULL for named streams.
     * @param allocationSize      Allocation size for the newly created file.
     */
    FileInfo create(FSP_FILE_SYSTEM fileSystem,
                    String fileName,
                    Set<CreateOptions> createOptions,
                    int grantedAccess,
                    Set<FileAttributes> fileAttributes,
                    Pointer pSecurityDescriptor /* (actual pointer is a PSECURITY_DESCRIPTOR which is a PVOID) */,
                    long allocationSize)
            throws NTStatusException;

    /**
     * Open a file or directory.
     *
     * @param fileSystem    The file system on which this request is posted.
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
    FileInfo open(FSP_FILE_SYSTEM fileSystem, String fileName, Set<CreateOptions> createOptions, int grantedAccess)
            throws NTStatusException;

    /**
     * Overwrite a file.
     *
     * @param fileSystem            The file system on which this request is posted.
     * @param fileName              The name of the file or directory being overwritten
     * @param fileAttributes        File attributes to apply to the overwritten file.
     * @param replaceFileAttributes When TRUE the existing file attributes should be replaced with the new ones.
     *                              When FALSE the existing file attributes should be merged (or'ed) with the new ones.
     * @param allocationSize        Allocation size for the overwritten file.
     */
    FileInfo overwrite(FSP_FILE_SYSTEM fileSystem,
                       String fileName,
                       Set<FileAttributes> fileAttributes,
                       boolean replaceFileAttributes,
                       long allocationSize)
            throws NTStatusException;

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
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file or directory to cleanup.
     * @param flags      These flags determine whether the file was modified and whether to delete the file.
     */
    void cleanup(FSP_FILE_SYSTEM fileSystem, String fileName, Set<CleanupFlags> flags);

    /**
     * Close a file.
     *
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file or directory to be closed.
     */
    void close(FSP_FILE_SYSTEM fileSystem, String fileName);

    /**
     * Read a file.
     * <p>
     * NOTE: STATUS_PENDING is supported allowing for asynchronous operation.
     *
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file to be read.
     * @param pBuffer    Pointer to a buffer that will receive the results of the read operation.
     * @param offset     Offset within the file to read from.
     * @param length     Length of data to read.
     */
    long read(FSP_FILE_SYSTEM fileSystem, String fileName, Pointer pBuffer, long offset, long length)
            throws NTStatusException;

    /**
     * Write a file.
     * <p>
     * NOTE: STATUS_PENDING is supported allowing for asynchronous operation.
     *
     * @param fileSystem       The file system on which this request is posted.
     * @param fileName         The name of the file to be written.
     * @param pBuffer          Pointer to a buffer that contains the data to write.
     * @param offset           Offset within the file to write to.
     * @param length           Length of data to write.
     * @param writeToEndOfFile When TRUE the file system must write to the current end of file. In this case the Offset
     *                         parameter will contain the value -1.
     * @param constrainedIo    When TRUE the file system must not extend the file (i.e. change the file size).
     */
    long write(FSP_FILE_SYSTEM fileSystem,
               String fileName,
               Pointer pBuffer,
               long offset,
               long length,
               boolean writeToEndOfFile,
               boolean constrainedIo
    ) throws NTStatusException;

    /**
     * Flush a file or volume.
     * <p>
     * Note that the FSD will also flush all file/volume caches prior to invoking this operation.
     *
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file to be flushed. When NULL the whole volume is being flushed.
     */
    void flush(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException;

    /**
     * Get file or directory information.
     *
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file or directory to get information for.
     */
    FileInfo getFileInfo(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException;

    /**
     * Set file or directory basic information.
     *
     * @param fileSystem     The file system on which this request is posted.
     * @param fileName       The name of the file or directory to set information for.
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
    FileInfo setBasicInfo(FSP_FILE_SYSTEM fileSystem,
                          String fileName,
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
     * @param fileSystem        The file system on which this request is posted.
     * @param fileName          The name of the file to set the file/allocation size for.
     * @param newSize           New file/allocation size to apply to the file.
     * @param setAllocationSize If TRUE, then the allocation size is being set. if FALSE, then the file size is being set.
     */
    FileInfo setFileSize(FSP_FILE_SYSTEM fileSystem, String fileName, long newSize, boolean setAllocationSize)
            throws NTStatusException;

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
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file or directory to test for deletion.
     */
    void canDelete(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException;

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
     * @param fileSystem      The file system on which this request is posted.
     * @param fileName        The current name of the file or directory to rename.
     * @param newFileName     The new name for the file or directory.
     * @param replaceIfExists Whether to replace a file that already exists at NewFileName.
     */
    void rename(FSP_FILE_SYSTEM fileSystem, String fileName, String newFileName, boolean replaceIfExists)
            throws NTStatusException;

    /**
     * Get file or directory security descriptor.
     *
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the file or directory to get the security descriptor for.
     * @return result with:
     * <ul>
     *     <li>STATUS_SUCCESS(0) and (non-null) security descriptor and size</li>
     *     <li>OR error code and null security descriptor and size</li>
     * </ul>
     */
    ResultSecurity getSecurity(FSP_FILE_SYSTEM fileSystem, String fileName);

    /**
     * Set file or directory security descriptor. See FspSetSecurityDescriptor or FspDeleteSecurityDescriptor
     * for more details.
     *
     * @param fileSystem              The file system on which this request is posted.
     * @param fileName                The name of the file or directory to set the security descriptor for.
     * @param securityInformation     Describes what parts of the file or directory security descriptor should
     *                                be modified.
     * @param pModificationDescriptor Describes the modifications to apply to the file or directory security descriptor.
     * @return result with:
     * <ul>
     *    <li>STATUS_SUCCESS(0)</li>
     *    <li>OR error code</li>
     * </ul>
     */
    Result setSecurity(FSP_FILE_SYSTEM fileSystem,
                       String fileName,
                       int securityInformation,
                       Pointer pModificationDescriptor /* (actual pointer is a PSECURITY_DESCRIPTOR which is a PVOID) */
    );

    /**
     * Read a directory. Returns a list of FileInfo.
     * <p>
     * NOTE: STATUS_PENDING is supported allowing for asynchronous operation.
     *
     * @param fileSystem The file system on which this request is posted.
     * @param fileName   The name of the directory to be read.
     * @param pattern    The pattern to match against files in this directory. Can be NULL. The file system
     *                   can choose to ignore this parameter as the FSD will always perform its own pattern
     *                   matching on the returned results.
     * @param marker     A file name that marks where in the directory to start reading. Files with names
     *                   that are greater than (not equal to) this marker (in the directory order determined
     *                   by the file system) should be returned. Can be NULL.
     */
    List<FileInfo> readDirectory(FSP_FILE_SYSTEM fileSystem, String fileName, String pattern, String marker)
            throws NTStatusException;
}
