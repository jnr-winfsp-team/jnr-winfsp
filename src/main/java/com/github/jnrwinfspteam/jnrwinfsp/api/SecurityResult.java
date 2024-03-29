package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.util.Objects;
import java.util.Set;

public final class SecurityResult {

    private final byte[] securityDescriptor;
    private final Set<FileAttributes> fileAttributes;

    public SecurityResult(byte[] securityDescriptor, Set<FileAttributes> fileAttributes) {
        this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
        this.fileAttributes = Objects.requireNonNull(fileAttributes);
    }

    public byte[] getSecurityDescriptor() {
        return securityDescriptor;
    }

    public Set<FileAttributes> getFileAttributes() {
        return fileAttributes;
    }
}
