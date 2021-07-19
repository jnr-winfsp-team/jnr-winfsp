package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.result.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import jnr.ffi.Pointer;

/**
 * Extend this class and override only those methods you wish to implement. The remaining methods
 * will never be called.
 */
public class WinFspStubFS extends AbstractWinFspFS {

    @Override
    @NotImplemented
    public ResultVolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultVolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultSecurityAndAttributes getSecurityByName(FSP_FILE_SYSTEM fileSystem, String fileName) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfoAndContext create(FSP_FILE_SYSTEM fileSystem,
                                           String fileName,
                                           int createOptions,
                                           int grantedAccess,
                                           int fileAttributes,
                                           Pointer pSecurityDescriptor,
                                           long allocationSize) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfoAndContext open(FSP_FILE_SYSTEM fileSystem,
                                         String fileName,
                                         int createOptions,
                                         int grantedAccess) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfo overwrite(FSP_FILE_SYSTEM fileSystem,
                                    FileContext fileContext,
                                    int fileAttributes,
                                    boolean replaceFileAttributes,
                                    long allocationSize) {
        return null;
    }

    @Override
    @NotImplemented
    public void cleanup(FSP_FILE_SYSTEM fileSystem, FileContext fileContext, String fileName, long flags) {

    }

    @Override
    @NotImplemented
    public void close(FSP_FILE_SYSTEM fileSystem, FileContext fileContext) {

    }

    @Override
    @NotImplemented
    public ResultRead read(FSP_FILE_SYSTEM fileSystem,
                           FileContext fileContext,
                           Pointer pBuffer,
                           long offset,
                           long length) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfoWrite write(FSP_FILE_SYSTEM fileSystem,
                                     FileContext fileContext,
                                     Pointer pBuffer,
                                     long offset,
                                     long length,
                                     boolean writeToEndOfFile,
                                     boolean constrainedIo) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfo flush(FSP_FILE_SYSTEM fileSystem, FileContext fileContext) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfo getFileInfo(FSP_FILE_SYSTEM fileSystem, FileContext pFileContext) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfo setBasicInfo(FSP_FILE_SYSTEM fileSystem,
                                       FileContext fileContext,
                                       int fileAttributes,
                                       long creationTime,
                                       long lastAccessTime,
                                       long lastWriteTime,
                                       long changeTime) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultFileInfo setFileSize(FSP_FILE_SYSTEM fileSystem,
                                      FileContext fileContext,
                                      long newSize,
                                      boolean setAllocationSize) {
        return null;
    }

    @Override
    @NotImplemented
    public Result canDelete(FSP_FILE_SYSTEM fileSystem, FileContext fileContext, String fileName) {
        return null;
    }

    @Override
    @NotImplemented
    public Result rename(FSP_FILE_SYSTEM fileSystem,
                         FileContext fileContext,
                         String fileName,
                         String newFileName,
                         boolean replaceIfExists) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultSecurity getSecurity(FSP_FILE_SYSTEM fileSystem, FileContext fileContext) {
        return null;
    }

    @Override
    @NotImplemented
    public Result setSecurity(FSP_FILE_SYSTEM fileSystem,
                              FileContext fileContext,
                              int securityInformation,
                              Pointer pModificationDescriptor) {
        return null;
    }

    @Override
    @NotImplemented
    public ResultRead readDirectory(FSP_FILE_SYSTEM fileSystem,
                                    FileContext fileContext,
                                    String pattern,
                                    String marker,
                                    Pointer pBuffer,
                                    long length) {
        return null;
    }
}
