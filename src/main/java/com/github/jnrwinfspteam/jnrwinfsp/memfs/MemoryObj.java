package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.api.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.api.FileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.api.ReparsePoint;
import com.github.jnrwinfspteam.jnrwinfsp.api.WinSysTime;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public abstract class MemoryObj {
    private final MemoryObj parent;
    private Path path;
    private final Set<FileAttributes> fileAttributes;
    private String securityDescriptor;
    private byte[] reparseData;
    private int reparseTag;
    private WinSysTime creationTime;
    private WinSysTime lastAccessTime;
    private WinSysTime lastWriteTime;
    private WinSysTime changeTime;
    private long indexNumber;

    public MemoryObj(MemoryObj parent, Path path, String securityDescriptor, ReparsePoint reparsePoint) {
        this.parent = parent;
        this.path = Objects.requireNonNull(path);
        this.fileAttributes = EnumSet.noneOf(FileAttributes.class);
        this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
        this.reparseData = null;
        this.reparseTag = 0;
        WinSysTime now = WinSysTime.now();
        this.creationTime = now;
        this.lastAccessTime = now;
        this.lastWriteTime = now;
        this.changeTime = now;
        this.indexNumber = 0;

        if (reparsePoint != null) {
            this.reparseData = reparsePoint.getData();
            this.reparseTag = reparsePoint.getTag();
            fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_REPARSE_POINT);
        }
    }

    public final Path getPath() {
        return path;
    }

    public final String getName() {
        return path.getNameCount() > 0 ? path.getFileName().toString() : null;
    }

    public final void setPath(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    public final MemoryObj getParent() {
        return parent;
    }

    public final Set<FileAttributes> getFileAttributes() {
        return fileAttributes;
    }

    public final String getSecurityDescriptor() {
        return securityDescriptor;
    }

    public final void setSecurityDescriptor(String securityDescriptor) {
        this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
    }

    public final byte[] getReparseData() {
        return reparseData;
    }

    public final void setReparseData(byte[] reparseData) {
        this.reparseData = reparseData;
    }

    public final int getReparseTag() {
        return reparseTag;
    }

    public final void setReparseTag(int reparseTag) {
        this.reparseTag = reparseTag;
    }

    public final void setCreationTime(WinSysTime time) {
        this.creationTime = Objects.requireNonNull(time);
    }

    public final void setAccessTime(WinSysTime time) {
        this.lastAccessTime = Objects.requireNonNull(time);
    }

    public final void setWriteTime(WinSysTime time) {
        this.lastWriteTime = Objects.requireNonNull(time);
    }

    public final void setChangeTime(WinSysTime time) {
        this.changeTime = Objects.requireNonNull(time);
    }

    public final void setIndexNumber(long indexNumber) {
        this.indexNumber = indexNumber;
    }

    public abstract int getAllocationSize();

    public abstract int getFileSize();


    public final FileInfo generateFileInfo() {
        return generateFileInfo(getPath().toString());
    }

    public final FileInfo generateFileInfo(String filePath) {
        FileInfo res = new FileInfo(filePath);
        res.getFileAttributes().addAll(fileAttributes);
        res.setAllocationSize(getAllocationSize());
        res.setFileSize(getFileSize());
        res.setCreationTime(creationTime);
        res.setLastAccessTime(lastAccessTime);
        res.setLastWriteTime(lastWriteTime);
        res.setChangeTime(changeTime);
        res.setReparseTag(reparseTag);
        res.setIndexNumber(indexNumber);
        return res;
    }

    public final void touch() {
        WinSysTime now = WinSysTime.now();
        setAccessTime(now);
        setWriteTime(now);
        setChangeTime(now);
    }

    public final void touchParent() {
        MemoryObj parent = getParent();
        if (parent != null)
            parent.touch();
    }
}
