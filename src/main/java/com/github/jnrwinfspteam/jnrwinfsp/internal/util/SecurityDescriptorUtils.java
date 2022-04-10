package com.github.jnrwinfspteam.jnrwinfsp.internal.util;

import com.github.jnrwinfspteam.jnrwinfsp.api.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibAdvapi32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

public class SecurityDescriptorUtils {

    public static byte[] toBytes(Pointer pSecurityDescriptor) {
        int length = LibAdvapi32.INSTANCE.GetSecurityDescriptorLength(pSecurityDescriptor);
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
                                byte[] securityDescriptor,
                                int securityInformation,
                                Pointer pModificationDescriptor) throws NTStatusException {

        // Put the security descriptor string in a pointer
        Pointer pSD = PointerUtils.fromBytes(runtime, securityDescriptor);
        try {
            PointerByReference outDescriptor = new PointerByReference();
            int status = LibWinFsp.INSTANCE.FspSetSecurityDescriptor(
                    pSD,
                    securityInformation,
                    pModificationDescriptor,
                    outDescriptor
            );

            if (status != 0)
                throw new NTStatusException(status);

            return toBytes(outDescriptor.getValue());
        } finally {
            PointerUtils.freeBytesPointer(pSD); // avoid memory leak
        }
    }

    public static Pointer getLocalWellKnownSID(Runtime runtime, int wellKnownSID) throws NTStatusException {
        Pointer pSize = PointerUtils.allocateMemory(runtime, Integer.BYTES);
        try {
            int ret = LibAdvapi32.INSTANCE.CreateWellKnownSid(
                    wellKnownSID,
                    null,
                    null,
                    pSize
            );
            if (PointerUtils.BOOL(ret))
                throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR

            Pointer pSID = PointerUtils.allocateMemory(runtime, pSize.getInt(0));
            ret = LibAdvapi32.INSTANCE.CreateWellKnownSid(
                    wellKnownSID,
                    null,
                    pSID,
                    pSize
            );
            if (!PointerUtils.BOOL(ret))
                throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR

            return pSID;
        } finally {
            PointerUtils.freeMemory(pSize);
        }
    }

    public static Pointer setOwnerAndGroup(Runtime runtime,
                                           Pointer pSecurityDescriptor,
                                           Pointer ownerSID,
                                           Pointer groupSID) throws NTStatusException {

        Pointer pASDSize = PointerUtils.allocateMemory(runtime, Integer.BYTES);
        Pointer pSize1 = PointerUtils.allocateMemory(runtime, Integer.BYTES);
        Pointer pSize2 = PointerUtils.allocateMemory(runtime, Integer.BYTES);
        Pointer pSize3 = PointerUtils.allocateMemory(runtime, Integer.BYTES);
        Pointer pSize4 = PointerUtils.allocateMemory(runtime, Integer.BYTES);
        try {
            int ret = LibAdvapi32.INSTANCE.MakeAbsoluteSD(
                    pSecurityDescriptor,
                    null, pASDSize,
                    null, pSize1,
                    null, pSize2,
                    null, pSize3,
                    null, pSize4
            );
            if (PointerUtils.BOOL(ret))
                throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR

            Pointer pASD = PointerUtils.allocateMemory(runtime, pASDSize.getInt(0));
            Pointer pDiscard1 = PointerUtils.allocateMemory(runtime, pSize1.getInt(0));
            Pointer pDiscard2 = PointerUtils.allocateMemory(runtime, pSize2.getInt(0));
            Pointer pDiscard3 = PointerUtils.allocateMemory(runtime, pSize3.getInt(0));
            Pointer pDiscard4 = PointerUtils.allocateMemory(runtime, pSize4.getInt(0));
            try {
                ret = LibAdvapi32.INSTANCE.MakeAbsoluteSD(
                        pSecurityDescriptor,
                        pASD, pASDSize,
                        pDiscard1, pSize1,
                        pDiscard2, pSize2,
                        pDiscard3, pSize3,
                        pDiscard4, pSize4
                );
                if (!PointerUtils.BOOL(ret)) {
                    throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR
                }

                ret = LibAdvapi32.INSTANCE.SetSecurityDescriptorOwner(pASD, ownerSID, 0);
                if (!PointerUtils.BOOL(ret))
                    throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR

                ret = LibAdvapi32.INSTANCE.SetSecurityDescriptorGroup(pASD, groupSID, 0);
                if (!PointerUtils.BOOL(ret))
                    throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR

                Pointer pRSDSize = PointerUtils.allocateMemory(runtime, Integer.BYTES);
                try {
                    ret = LibAdvapi32.INSTANCE.MakeSelfRelativeSD(pASD, null, pRSDSize);
                    if (PointerUtils.BOOL(ret))
                        throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR

                    Pointer pRSD = PointerUtils.allocateMemory(runtime, pRSDSize.getInt(0));
                    ret = LibAdvapi32.INSTANCE.MakeSelfRelativeSD(pASD, pRSD, pRSDSize);
                    if (!PointerUtils.BOOL(ret))
                        throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR

                    return pRSD;
                } finally {
                    PointerUtils.freeMemory(pRSDSize); // avoid memory leak
                }
            } finally {
                PointerUtils.freeMemory(pASD); // avoid memory leak
                PointerUtils.freeMemory(pDiscard1); // avoid memory leak
                PointerUtils.freeMemory(pDiscard2); // avoid memory leak
                PointerUtils.freeMemory(pDiscard3); // avoid memory leak
                PointerUtils.freeMemory(pDiscard4); // avoid memory leak
            }
        } finally {
            PointerUtils.freeMemory(pASDSize); // avoid memory leak
            PointerUtils.freeMemory(pSize1); // avoid memory leak
            PointerUtils.freeMemory(pSize2); // avoid memory leak
            PointerUtils.freeMemory(pSize3); // avoid memory leak
            PointerUtils.freeMemory(pSize4); // avoid memory leak
        }
    }
}
