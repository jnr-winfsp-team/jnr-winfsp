package com.github.jnrwinfspteam.jnrwinfsp.internal.lib;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.u_int32_t;

public interface LibAdvapi32 {

    static final LibAdvapi32 INSTANCE = LibraryLoader.create(LibAdvapi32.class)
            .library("Advapi32.dll")
            .failImmediately()
            .load();

    int SDDL_REVISION_1 = 1;

    int OWNER_SECURITY_INFORMATION = 0x00000001;
    int GROUP_SECURITY_INFORMATION = 0x00000002;
    int DACL_SECURITY_INFORMATION = 0x00000004;
    int SACL_SECURITY_INFORMATION = 0x00000008;

    int WinBuiltinAdministratorsSid = 26;

    /* BOOL */ int ConvertStringSecurityDescriptorToSecurityDescriptorW(
            Pointer /* LCWSTR */ pStringSecurityDescriptor,
            @u_int32_t int stringSDRevision,
            @Out PointerByReference /* PSECURITY_DESCRIPTOR */ ppSecurityDescriptor,
            Pointer /* ULONG */ pSecurityDescriptorSize
    );

    /* BOOL */ int ConvertSecurityDescriptorToStringSecurityDescriptorW(
            Pointer /* SECURITY_DESCRIPTOR */ pSecurityDescriptor,
            @u_int32_t int requestedStringSDRevision,
            @u_int32_t int securityInformation,
            @Out PointerByReference /* LPWSTR */ ppStringSecurityDescriptor,
            Pointer /* ULONG */ stringSecurityDescriptorLen
    );

    int GetSecurityDescriptorLength(
            /* PSECURITY_DESCRIPTOR */ Pointer pSecurityDescriptor
    );

    /* BOOL */ int CreateWellKnownSid(
            int /* WELL_KNOWN_SID_TYPE */ wellKnownSidType,
            Pointer /* SID */ domainSid,
            @Out Pointer /* SID */ pSid,
            @Out Pointer /* DWORD */ pCbSid
    );

    /* BOOL */ int MakeAbsoluteSD(
            Pointer /* SECURITY_DESCRIPTOR */ pSelfRelativeSecurityDescriptor,
            @Out Pointer /* SECURITY_DESCRIPTOR */ pAbsoluteSecurityDescriptor,
            @Out Pointer /* DWORD */ pAbsoluteSecurityDescriptorSize,
            @Out Pointer /* ACL */ pDacl,
            @Out Pointer /* DWORD */ pDaclSize,
            @Out Pointer /* ACL */ pSacl,
            @Out Pointer /* DWORD */ pSaclSize,
            @Out Pointer /* SID */ pOwner,
            @Out Pointer /* DWORD */ pOwnerSize,
            @Out Pointer /* SID */ pPrimaryGroup,
            @Out Pointer /* DWORD */ pPrimaryGroupSize
    );

    /* BOOL */ int MakeSelfRelativeSD(
            Pointer /* SECURITY_DESCRIPTOR */ pAbsoluteSecurityDescriptor,
            @Out Pointer /* SECURITY_DESCRIPTOR */ pSelfRelativeSecurityDescriptor,
            @Out Pointer /* DWORD */ pBufferLength
    );

    /* BOOL */ int SetSecurityDescriptorOwner(
            @Out Pointer /* SECURITY_DESCRIPTOR */ pSecurityDescriptor,
            Pointer /* SID */ pOwner,
            int /* BOOL */ bOwnerDefaulted
    );

    /* BOOL */ int SetSecurityDescriptorGroup(
            @Out Pointer /* SECURITY_DESCRIPTOR */ pSecurityDescriptor,
            Pointer /* SID */ pGroup,
            int /* BOOL */ bGroupDefaulted
    );
}