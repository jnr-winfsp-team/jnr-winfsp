package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.util.Objects;

public final class OpenResult {

    private final long fileHandle;
    private final FileInfo fileInfo;

    public OpenResult(long fileHandle, FileInfo fileInfo) {
        this.fileHandle = fileHandle;
        this.fileInfo = Objects.requireNonNull(fileInfo);
    }

    public long getFileHandle() {
        return fileHandle;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }
}
