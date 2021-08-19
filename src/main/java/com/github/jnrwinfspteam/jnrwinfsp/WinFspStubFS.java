package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.flags.CleanupFlags;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;
import jnr.ffi.Pointer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Extend this class and override only the operations you wish to implement. The remaining operations
 * will never be called.
 */
public class WinFspStubFS extends AbstractWinFspFS {

    @Override
    @NotImplemented
    public VolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public VolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public Optional<SecurityResult> getSecurityByName(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo create(FSP_FILE_SYSTEM fileSystem,
                           String fileName,
                           Set<CreateOptions> createOptions,
                           int grantedAccess,
                           Set<FileAttributes> fileAttributes,
                           String securityDescriptorString,
                           long allocationSize,
                           byte[] reparseData,
                           int reparseTag) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo open(FSP_FILE_SYSTEM fileSystem,
                         String fileName,
                         Set<CreateOptions> createOptions,
                         int grantedAccess) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo overwrite(FSP_FILE_SYSTEM fileSystem,
                              String fileName,
                              Set<FileAttributes> fileAttributes,
                              boolean replaceFileAttributes,
                              long allocationSize) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void cleanup(FSP_FILE_SYSTEM fileSystem, String fileName, Set<CleanupFlags> flags) {

    }

    @Override
    @NotImplemented
    public void close(FSP_FILE_SYSTEM fileSystem, String fileName) {

    }

    @Override
    @NotImplemented
    public long read(FSP_FILE_SYSTEM fileSystem,
                     String fileName,
                     Pointer pBuffer,
                     long offset,
                     int length) throws NTStatusException {
        return 0;
    }

    @Override
    @NotImplemented
    public WriteResult write(FSP_FILE_SYSTEM fileSystem,
                             String fileName,
                             Pointer pBuffer,
                             long offset,
                             int length,
                             boolean writeToEndOfFile,
                             boolean constrainedIo) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo flush(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo getFileInfo(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo setBasicInfo(FSP_FILE_SYSTEM fileSystem,
                                 String fileName,
                                 Set<FileAttributes> fileAttributes,
                                 WinSysTime creationTime,
                                 WinSysTime lastAccessTime,
                                 WinSysTime lastWriteTime,
                                 WinSysTime changeTime) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo setFileSize(FSP_FILE_SYSTEM fileSystem,
                                String fileName,
                                long newSize,
                                boolean setAllocationSize) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void canDelete(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

    }

    @Override
    @NotImplemented
    public void rename(FSP_FILE_SYSTEM fileSystem,
                       String fileName,
                       String newFileName,
                       boolean replaceIfExists) throws NTStatusException {

    }

    @Override
    @NotImplemented
    public String getSecurity(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void setSecurity(FSP_FILE_SYSTEM fileSystem,
                            String fileName,
                            String securityDescriptorStr) throws NTStatusException {
    }

    @Override
    @NotImplemented
    public List<FileInfo> readDirectory(FSP_FILE_SYSTEM fileSystem,
                                        String fileName,
                                        String pattern,
                                        String marker) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo getDirInfoByName(FSP_FILE_SYSTEM fileSystem, String parentDirName, String fileName)
            throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public byte[] getReparsePointData(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void setReparsePoint(FSP_FILE_SYSTEM fileSystem, String fileName, byte[] reparseData, int reparseTag)
            throws NTStatusException {
    }

    @Override
    @NotImplemented
    public void deleteReparsePoint(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

    }
}
