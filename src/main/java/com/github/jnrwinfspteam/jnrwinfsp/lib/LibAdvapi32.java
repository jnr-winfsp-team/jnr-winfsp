package com.github.jnrwinfspteam.jnrwinfsp.lib;

import jnr.ffi.Pointer;
import jnr.ffi.types.u_int32_t;

public interface LibAdvapi32 {
    int SDDL_REVISION_1 = 1;

    boolean ConvertStringSecurityDescriptorToSecurityDescriptorW(
            Pointer /* LCWSTR */ pStringSecurityDescriptor,
            @u_int32_t int stringSDRevision,
            Pointer /* PSECURITY_DESCRIPTOR */ ppSecurityDescriptor,
            Pointer /* ULONG */ pSecurityDescriptorSize
    );
}