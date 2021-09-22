package com.github.jnrwinfspteam.jnrwinfsp.internal.util;

import com.github.jnrwinfspteam.jnrwinfsp.api.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibAdvapi32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

public class SecurityDescriptorUtils {

    public static byte[] toBytes(LibAdvapi32 libAdvapi32, Pointer pSecurityDescriptor) {
        int length = libAdvapi32.GetSecurityDescriptorLength(pSecurityDescriptor);
        return PointerUtils.getBytes(pSecurityDescriptor, 0, length);
    }

    public static void fromBytes(Runtime runtime,
                                 byte[] securityDescriptor,
                                 Pointer outPSecurityDescriptor,
                                 Pointer outPSecurityDescriptorSize) throws NTStatusException {

        Pointer pSD = PointerUtils.fromBytes(runtime, securityDescriptor);
        try {
            // Put the converted security descriptor (and its size) in the output arguments
            if (outPSecurityDescriptorSize != null) {
                int sdSize = securityDescriptor.length;
                if (sdSize > outPSecurityDescriptorSize.getInt(0)) {
                    // In case of overflow error, WinFsp will retry with a new
                    // allocation based on `pSecurityDescriptorSize`. Hence we
                    // must update this value to the required size.
                    outPSecurityDescriptorSize.putInt(0, sdSize);
                    throw new NTStatusException(0x80000005); // STATUS_BUFFER_OVERFLOW
                }

                outPSecurityDescriptorSize.putInt(0, sdSize);
                if (outPSecurityDescriptor != null) {
                    outPSecurityDescriptor.transferFrom(0, pSD, 0, sdSize);
                }
            }
        } finally {
            PointerUtils.freeBytesPointer(pSD);
        }
    }

    public static byte[] modify(Runtime runtime,
                                LibWinFsp libWinFsp,
                                LibAdvapi32 libAdvapi32,
                                byte[] securityDescriptor,
                                int securityInformation,
                                Pointer pModificationDescriptor) throws NTStatusException {

        // Put the security descriptor string in a pointer
        Pointer pSD = PointerUtils.fromBytes(runtime, securityDescriptor);
        try {
            PointerByReference outDescriptor = new PointerByReference();
            int status = libWinFsp.FspSetSecurityDescriptor(
                    pSD,
                    securityInformation,
                    pModificationDescriptor,
                    outDescriptor
            );

            if (status != 0)
                throw new NTStatusException(status);

            return toBytes(libAdvapi32, outDescriptor.getValue());
        } finally {
            PointerUtils.freeBytesPointer(pSD); // avoid memory leak
        }
    }
}
