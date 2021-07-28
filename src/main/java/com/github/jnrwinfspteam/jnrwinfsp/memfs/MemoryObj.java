package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.FileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public abstract class MemoryObj {
    private Path path;
    private String securityDescriptor;
    private final Set<FileAttributes> fileAttributes;
    private WinSysTime creationTime;
    private WinSysTime lastAccessTime;
    private WinSysTime lastWriteTime;
    private WinSysTime changeTime;
    private long indexNumber;

    public MemoryObj(Path path, String securityDescriptor) {
        this.path = Objects.requireNonNull(path);
        this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
        this.fileAttributes = EnumSet.noneOf(FileAttributes.class);
        WinSysTime now = WinSysTime.now();
        this.creationTime = now;
        this.lastAccessTime = now;
        this.lastWriteTime = now;
        this.changeTime = now;
        this.indexNumber = 0;
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return path.getNameCount() > 0 ? path.getFileName().toString() : null;
    }

    public void setPath(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    public String getSecurityDescriptor() {
        return securityDescriptor;
    }

    public void setSecurityDescriptor(String securityDescriptor) {
        this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
    }

    public FileInfo generateFileInfo() {
        return generateFileInfo(getPath().toString());
    }

    public FileInfo generateFileInfo(String filePath) {
        FileInfo res = new FileInfo(filePath);
        res.getFileAttributes().addAll(fileAttributes);
        res.setAllocationSize(getAllocationSize());
        res.setFileSize(getFileSize());
        res.setCreationTime(creationTime);
        res.setLastAccessTime(lastAccessTime);
        res.setLastWriteTime(lastWriteTime);
        res.setChangeTime(changeTime);
        res.setIndexNumber(indexNumber);
        res.setNormalizedName(filePath.toLowerCase(Locale.ROOT));
        return res;
    }

    protected final Set<FileAttributes> getFileAttributes() {
        return fileAttributes;
    }

    protected final void setCreationTime(WinSysTime time) {
        this.creationTime = Objects.requireNonNull(time);
    }

    protected final void setAccessTime(WinSysTime time) {
        this.lastAccessTime = Objects.requireNonNull(time);
    }

    protected final void setWriteTime(WinSysTime time) {
        this.lastWriteTime = Objects.requireNonNull(time);
    }

    protected final void setChangeTime(WinSysTime time) {
        this.changeTime = Objects.requireNonNull(time);
    }

    protected final void setIndexNumber(long indexNumber) {
        this.indexNumber = indexNumber;
    }

    protected abstract int getAllocationSize();

    protected abstract int getFileSize();
}
