package com.github.jnrwinfspteam.jnrwinfsp.api;

import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibAdvapi32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibKernel32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.PointerUtils;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.StringUtils;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

public final class SecurityDescriptorHandler {

    private static final int REQUESTED_SECURITY_INFORMATION =
            LibAdvapi32.OWNER_SECURITY_INFORMATION
                    | LibAdvapi32.GROUP_SECURITY_INFORMATION
                    | LibAdvapi32.DACL_SECURITY_INFORMATION
                    | LibAdvapi32.SACL_SECURITY_INFORMATION;

    public static byte[] securityDescriptorToBytes(String sd) throws NTStatusException {
        Runtime runtime = Runtime.getSystemRuntime();

        // Put the security descriptor string in an allocated pointer
        Pointer pStringSecurityDescriptor = StringUtils.toPointer(runtime, sd, true);

        // Prepare a pointer to a pointer in order to store the converted security descriptor
        PointerByReference ppSD = new PointerByReference();

        // Allocate a pointer in order to store the size of the converted security descriptor
        int uLongSize = runtime.findType(NativeType.ULONG).size();
        Pointer psdSize = PointerUtils.allocateMemory(runtime, uLongSize);

        // Do the conversion from a string to a security descriptor
        boolean res = bool(LibAdvapi32.INSTANCE.ConvertStringSecurityDescriptorToSecurityDescriptorW(
                pStringSecurityDescriptor,
                LibAdvapi32.SDDL_REVISION_1,
                ppSD,
                psdSize)
        );
        if (!res) {
            StringUtils.freeStringPointer(pStringSecurityDescriptor); // avoid memory leak
            PointerUtils.freeMemory(psdSize); // avoid memory leak
            throw new NTStatusException(LibWinFsp.INSTANCE.FspNtStatusFromWin32(runtime.getLastError()));
        }

        try {
            Pointer pSD = ppSD.getValue();
            int sdSize = psdSize.getInt(0);
            return PointerUtils.getBytes(pSD, 0, sdSize);
        } finally {
            StringUtils.freeStringPointer(pStringSecurityDescriptor); // avoid memory leak
            PointerUtils.freeMemory(psdSize); // avoid memory leak
            LibKernel32.INSTANCE.LocalFree(ppSD.getValue()); // avoid memory leak
        }
    }

    public static String securityDescriptorToString(byte[] sd) throws NTStatusException {
        Runtime runtime = Runtime.getSystemRuntime();

        Pointer pSD = PointerUtils.fromBytes(runtime, sd);

        // Prepare a pointer to a pointer in order to store the converted security descriptor string
        PointerByReference ppSDString = new PointerByReference();

        if (!bool(LibAdvapi32.INSTANCE.ConvertSecurityDescriptorToStringSecurityDescriptorW(
                pSD,
                LibAdvapi32.SDDL_REVISION_1,
                REQUESTED_SECURITY_INFORMATION,
                ppSDString,
                null))
        ) {
            PointerUtils.freeBytesPointer(pSD); // avoid memory leak
            throw new NTStatusException(LibWinFsp.INSTANCE.FspNtStatusFromWin32(runtime.getLastError()));
        }

        try {
            return StringUtils.fromPointer(ppSDString.getValue());
        } finally {
            PointerUtils.freeBytesPointer(pSD); // avoid memory leak
            LibKernel32.INSTANCE.LocalFree(ppSDString.getValue()); // avoid memory leak
        }
    }

    private static boolean bool(int val) {
        return PointerUtils.BOOL(val);
    }

    private SecurityDescriptorHandler() {
        // not instantiable
    }
}
