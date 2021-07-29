package com.github.jnrwinfspteam.jnrwinfsp.result;

import java.util.Objects;

public final class SecurityResult {

    private final String securityDescriptor;
    private final FileInfo fileInfo;

    public SecurityResult(String securityDescriptor, FileInfo fileInfo) {
        this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
        this.fileInfo = Objects.requireNonNull(fileInfo);
    }

    public String getSecurityDescriptor() {
        return securityDescriptor;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }
}
