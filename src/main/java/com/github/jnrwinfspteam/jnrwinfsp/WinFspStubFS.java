package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.api.*;
import jnr.ffi.Pointer;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Extend this class and override only the operations you wish to implement. The remaining operations
 * will never be called.
 */
public class WinFspStubFS extends AbstractWinFspFS {

    @Override
    @NotImplemented
    public VolumeInfo getVolumeInfo() throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public VolumeInfo setVolumeLabel(String volumeLabel) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public Optional<SecurityResult> getSecurityByName(String fileName) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo create(
            String fileName,
            Set<CreateOptions> createOptions,
            int grantedAccess,
            Set<FileAttributes> fileAttributes,
            byte[] securityDescriptor,
            long allocationSize,
            ReparsePoint reparsePoint) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo open(
            String fileName,
            Set<CreateOptions> createOptions,
            int grantedAccess) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo overwrite(
            String fileName,
            Set<FileAttributes> fileAttributes,
            boolean replaceFileAttributes,
            long allocationSize) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void cleanup(OpenContext ctx, Set<CleanupFlags> flags) {

    }

    @Override
    @NotImplemented
    public void close(OpenContext ctx) {

    }

    @Override
    @NotImplemented
    public long read(
            String fileName,
            Pointer pBuffer,
            long offset,
            int length) throws NTStatusException {
        return 0;
    }

    @Override
    @NotImplemented
    public WriteResult write(
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
    public FileInfo flush(String fileName) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo getFileInfo(OpenContext ctx) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo setBasicInfo(
            OpenContext ctx,
            Set<FileAttributes> fileAttributes,
            WinSysTime creationTime,
            WinSysTime lastAccessTime,
            WinSysTime lastWriteTime,
            WinSysTime changeTime) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public FileInfo setFileSize(
            String fileName,
            long newSize,
            boolean setAllocationSize) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void canDelete(OpenContext ctx) throws NTStatusException {

    }

    @Override
    @NotImplemented
    public void rename(
            OpenContext ctx,
            String oldFileName,
            String newFileName,
            boolean replaceIfExists) throws NTStatusException {

    }

    @Override
    @NotImplemented
    public byte[] getSecurity(OpenContext ctx) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void setSecurity(
            OpenContext ctx,
            byte[] securityDescriptor) throws NTStatusException {
    }

    @Override
    @NotImplemented
    public void readDirectory(
            String dirName,
            String pattern,
            String marker,
            Predicate<FileInfo> consumer) throws NTStatusException {
    }

    @Override
    @NotImplemented
    public FileInfo getDirInfoByName(String parentDirName, String fileName)
            throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public byte[] getReparsePointData(OpenContext ctx) throws NTStatusException {
        return null;
    }

    @Override
    @NotImplemented
    public void setReparsePoint(OpenContext ctx, byte[] reparseData, int reparseTag)
            throws NTStatusException {
    }

    @Override
    @NotImplemented
    public void deleteReparsePoint(OpenContext ctx) throws NTStatusException {

    }
}
