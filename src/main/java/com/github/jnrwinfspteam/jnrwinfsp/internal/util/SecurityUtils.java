package com.github.jnrwinfspteam.jnrwinfsp.internal.util;

import com.github.jnrwinfspteam.jnrwinfsp.api.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibAdvapi32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibKernel32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

public final class SecurityUtils {
    private static final int REQUESTED_SECURITY_INFORMATION =
            LibAdvapi32.OWNER_SECURITY_INFORMATION
                    | LibAdvapi32.GROUP_SECURITY_INFORMATION
                    | LibAdvapi32.DACL_SECURITY_INFORMATION
                    | LibAdvapi32.SACL_SECURITY_INFORMATION;

    public static String toString(LibWinFsp libWinFsp,
                                  LibKernel32 libKernel32,
                                  LibAdvapi32 libAdvapi32,
                                  Pointer pSecurityDescriptor
    ) throws NTStatusException {

        Runtime runtime = Runtime.getSystemRuntime();

        // Prepare a pointer to a pointer in order to store the converted security descriptor string
        PointerByReference ppSDString = new PointerByReference();

        if (!bool(libAdvapi32.ConvertSecurityDescriptorToStringSecurityDescriptorW(
                pSecurityDescriptor,
                LibAdvapi32.SDDL_REVISION_1,
                REQUESTED_SECURITY_INFORMATION,
                ppSDString,
                null))
        ) {
            throw new NTStatusException(libWinFsp.FspNtStatusFromWin32(runtime.getLastError()));
        }

        try {
            return StringUtils.fromPointer(ppSDString.getValue());
        } finally {
            libKernel32.LocalFree(ppSDString.getValue()); // avoid memory leak
        }
    }

    public static void fromString(LibWinFsp libWinFsp,
                                  LibKernel32 libKernel32,
                                  LibAdvapi32 libAdvapi32,
                                  String securityDescriptorStr,
                                  Pointer outPSecurityDescriptor,
                                  Pointer outPSecurityDescriptorSize
    ) throws NTStatusException {

        Runtime runtime = Runtime.getSystemRuntime();

        // Put the security descriptor string in an allocated pointer
        Pointer pStringSecurityDescriptor = StringUtils.toPointer(runtime, securityDescriptorStr, true);

        // Prepare a pointer to a pointer in order to store the converted security descriptor
        PointerByReference ppSD = new PointerByReference();

        // Allocate a pointer in order to store the size of the converted security descriptor
        int uLongSize = runtime.findType(NativeType.ULONG).size();
        Pointer psdSize = PointerUtils.allocateMemory(runtime, uLongSize);

        // Do the conversion from a string to a security descriptor
        boolean res = bool(libAdvapi32.ConvertStringSecurityDescriptorToSecurityDescriptorW(
                pStringSecurityDescriptor,
                LibAdvapi32.SDDL_REVISION_1,
                ppSD,
                psdSize)
        );
        if (!res) {
            StringUtils.freeStringPointer(pStringSecurityDescriptor); // avoid memory leak
            PointerUtils.freeMemory(psdSize); // avoid memory leak
            throw new NTStatusException(libWinFsp.FspNtStatusFromWin32(runtime.getLastError()));
        }

        try {
            // Put the converted security descriptor (and its size) in the output arguments
            if (outPSecurityDescriptorSize != null) {
                int sdSize = psdSize.getInt(0);
                if (sdSize > outPSecurityDescriptorSize.getInt(0)) {
                    // In case of overflow error, WinFsp will retry with a new
                    // allocation based on `pSecurityDescriptorSize`. Hence we
                    // must update this value to the required size.
                    outPSecurityDescriptorSize.putInt(0, sdSize);
                    throw new NTStatusException(0x80000005); // STATUS_BUFFER_OVERFLOW
                }

                outPSecurityDescriptorSize.putInt(0, sdSize);
                if (outPSecurityDescriptor != null) {
                    outPSecurityDescriptor.transferFrom(0, ppSD.getValue(), 0, sdSize);
                }
            }
        } finally {
            StringUtils.freeStringPointer(pStringSecurityDescriptor); // avoid memory leak
            PointerUtils.freeMemory(psdSize); // avoid memory leak
            libKernel32.LocalFree(ppSD.getValue()); // avoid memory leak
        }
    }

    public static String modify(LibWinFsp libWinFsp,
                                LibKernel32 libKernel32,
                                LibAdvapi32 libAdvapi32,
                                String securityDescriptorStr,
                                int securityInformation,
                                Pointer pModificationDescriptor
    ) throws NTStatusException {

        Runtime runtime = Runtime.getSystemRuntime();

        // Put the security descriptor string in a pointer
        Pointer pStringSecurityDescriptor = StringUtils.toPointer(runtime, securityDescriptorStr, true);

        // Prepare a pointer to a pointer in order to store the converted security descriptor
        PointerByReference ppSD = new PointerByReference();

        // Do the conversion from a string to a security descriptor
        boolean res = bool(libAdvapi32.ConvertStringSecurityDescriptorToSecurityDescriptorW(
                pStringSecurityDescriptor,
                LibAdvapi32.SDDL_REVISION_1,
                ppSD,
                null)
        );
        if (!res) {
            StringUtils.freeStringPointer(pStringSecurityDescriptor); // avoid memory leak
            throw new NTStatusException(libWinFsp.FspNtStatusFromWin32(runtime.getLastError()));
        }

        try {
            PointerByReference outDescriptor = new PointerByReference();
            int status = libWinFsp.FspSetSecurityDescriptor(
                    ppSD.getValue(),
                    securityInformation,
                    pModificationDescriptor,
                    outDescriptor
            );

            if (status != 0)
                throw new NTStatusException(status);

            return toString(
                    libWinFsp,
                    libKernel32,
                    libAdvapi32,
                    outDescriptor.getValue()
            );
        } finally {
            StringUtils.freeStringPointer(pStringSecurityDescriptor); // avoid memory leak
            libKernel32.LocalFree(ppSD.getValue()); // avoid memory leak
        }
    }

    private static boolean bool(int val) {
        return PointerUtils.BOOL(val);
    }

    private SecurityUtils() {
        // not instantiable
    }
}
