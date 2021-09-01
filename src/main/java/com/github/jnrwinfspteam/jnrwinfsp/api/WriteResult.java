package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.util.Objects;

public final class WriteResult {

    private final long bytesTransferred;
    private final FileInfo fileInfo;

    public WriteResult(long bytesTransferred, FileInfo fileInfo) {
        this.bytesTransferred = bytesTransferred;
        this.fileInfo = Objects.requireNonNull(fileInfo);
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }
}
