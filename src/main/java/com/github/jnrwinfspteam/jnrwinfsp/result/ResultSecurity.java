package com.github.jnrwinfspteam.jnrwinfsp.result;

import jnr.ffi.Pointer;

public class ResultSecurity extends Result {

    private final Pointer securityDescriptor; /* (actual pointer is a PSECURITY_DESCRIPTOR which is a PVOID) */
    private final int securityDescriptorSize;

    public ResultSecurity(int ntStatus,
                          Pointer securityDescriptor,
                          int securityDescriptorSize) {
        super(ntStatus);
        this.securityDescriptor = securityDescriptor;
        this.securityDescriptorSize = securityDescriptorSize;
    }

    public final Pointer getSecurityDescriptor() {
        return securityDescriptor;
    }

    public final int getSecurityDescriptorSize() {
        return securityDescriptorSize;
    }
}
