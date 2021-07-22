package com.github.jnrwinfspteam.jnrwinfsp.result;

import jnr.ffi.Pointer;

public class ResultSecurityAndAttributes extends ResultSecurity {

    private final int fileAttributesOrReparsePointIndex;

    public ResultSecurityAndAttributes(Pointer securityDescriptor,
                                       int securityDescriptorSize,
                                       int fileAttributesOrReparsePointIndex) {
        this(0, securityDescriptor, securityDescriptorSize, fileAttributesOrReparsePointIndex);
    }

    public ResultSecurityAndAttributes(int ntStatus,
                                       Pointer securityDescriptor,
                                       int securityDescriptorSize,
                                       int fileAttributesOrReparsePointIndex) {
        super(ntStatus, securityDescriptor, securityDescriptorSize);
        this.fileAttributesOrReparsePointIndex = fileAttributesOrReparsePointIndex;
    }

    public final int getFileAttributes() {
        return fileAttributesOrReparsePointIndex;
    }

    public final int getReparsePointIndex() {
        return fileAttributesOrReparsePointIndex;
    }
}
