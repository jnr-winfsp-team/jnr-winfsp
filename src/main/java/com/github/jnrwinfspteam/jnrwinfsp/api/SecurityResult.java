package com.github.jnrwinfspteam.jnrwinfsp.api;

import com.github.jnrwinfspteam.jnrwinfsp.api.FileAttributes;

import java.util.Objects;
import java.util.Set;

public final class SecurityResult {

    private final String securityDescriptor;
    private final Set<FileAttributes> fileAttributes;

    public SecurityResult(String securityDescriptor, Set<FileAttributes> fileAttributes) {
        this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
        this.fileAttributes = Objects.requireNonNull(fileAttributes);
    }

    public String getSecurityDescriptor() {
        return securityDescriptor;
    }

    public Set<FileAttributes> getFileAttributes() {
        return fileAttributes;
    }
}
