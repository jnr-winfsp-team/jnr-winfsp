package com.github.jnrwinfspteam.jnrwinfsp.lib;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.u_int32_t;

public interface LibAdvapi32 {
    int SDDL_REVISION_1 = 1;

    int OWNER_SECURITY_INFORMATION = 0x00000001;
    int GROUP_SECURITY_INFORMATION = 0x00000002;
    int DACL_SECURITY_INFORMATION = 0x00000004;
    int SACL_SECURITY_INFORMATION = 0x00000008;

    boolean ConvertStringSecurityDescriptorToSecurityDescriptorW(
            Pointer /* LCWSTR */ pStringSecurityDescriptor,
            @u_int32_t int stringSDRevision,
            @Out PointerByReference /* PSECURITY_DESCRIPTOR */ ppSecurityDescriptor,
            Pointer /* ULONG */ pSecurityDescriptorSize
    );

    boolean ConvertSecurityDescriptorToStringSecurityDescriptorW(
            Pointer /* SECURITY_DESCRIPTOR */ pSecurityDescriptor,
            @u_int32_t int requestedStringSDRevision,
            @u_int32_t int securityInformation,
            @Out PointerByReference /* LPWSTR */ ppStringSecurityDescriptor,
            Pointer /* ULONG */ stringSecurityDescriptorLen
    );
}