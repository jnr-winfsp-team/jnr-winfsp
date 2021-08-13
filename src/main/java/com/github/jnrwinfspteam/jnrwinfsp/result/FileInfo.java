package com.github.jnrwinfspteam.jnrwinfsp.result;

import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class FileInfo {
    private final String fileName;
    private final Set<FileAttributes> fileAttributes;
    private long allocationSize;
    private long fileSize;
    private WinSysTime creationTime;
    private WinSysTime lastAccessTime;
    private WinSysTime lastWriteTime;
    private WinSysTime changeTime;
    private String normalizedName;
    private int reparseTag;
    private long indexNumber;
    private int eaSize;
    private final int hardLinks = 0; /* unimplemented: set to 0 */

    public FileInfo(String fileName) {
        this.fileName = Objects.requireNonNull(fileName);
        this.fileAttributes = EnumSet.noneOf(FileAttributes.class);
        WinSysTime now = WinSysTime.now();
        this.creationTime = now;
        this.lastAccessTime = now;
        this.lastWriteTime = now;
        this.changeTime = now;
        this.normalizedName = this.fileName;
        this.reparseTag = 0;
        this.indexNumber = 0;
        this.eaSize = 0;
    }

    public String getFileName() {
        return fileName;
    }

    public final Set<FileAttributes> getFileAttributes() {
        return fileAttributes;
    }

    public final long getAllocationSize() {
        return allocationSize;
    }

    public final void setAllocationSize(long allocationSize) {
        this.allocationSize = allocationSize;
    }

    public final long getFileSize() {
        return fileSize;
    }

    public final void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public final WinSysTime getCreationTime() {
        return creationTime;
    }

    public final void setCreationTime(WinSysTime creationTime) {
        this.creationTime = Objects.requireNonNull(creationTime);
    }

    public final WinSysTime getLastAccessTime() {
        return lastAccessTime;
    }

    public final void setLastAccessTime(WinSysTime lastAccessTime) {
        this.lastAccessTime = Objects.requireNonNull(lastAccessTime);
    }

    public final WinSysTime getLastWriteTime() {
        return lastWriteTime;
    }

    public final void setLastWriteTime(WinSysTime lastWriteTime) {
        this.lastWriteTime = Objects.requireNonNull(lastWriteTime);
    }

    public final WinSysTime getChangeTime() {
        return changeTime;
    }

    public final void setChangeTime(WinSysTime changeTime) {
        this.changeTime = Objects.requireNonNull(changeTime);
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = Objects.requireNonNull(normalizedName);
    }

    public final int getReparseTag() {
        return reparseTag;
    }

    public final void setReparseTag(int reparseTag) {
        this.reparseTag = reparseTag;
    }

    public final long getIndexNumber() {
        return indexNumber;
    }

    public final void setIndexNumber(long indexNumber) {
        this.indexNumber = indexNumber;
    }

    public final int getHardLinks() {
        return hardLinks;
    }

    public final int getEaSize() {
        return eaSize;
    }

    public final void setEaSize(int eaSize) {
        this.eaSize = eaSize;
    }

    @Override
    public String toString() {
        return fileName;
    }
}
