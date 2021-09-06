package com.github.jnrwinfspteam.jnrwinfsp.internal.util;

import jnr.ffi.Pointer;

public class PointerUtils {

    public static byte[] getBytes(Pointer pBuf, long offset, int size) {
        byte[] bytes = new byte[size];
        pBuf.get(offset, bytes, 0, bytes.length);
        return bytes;
    }

    public static boolean BOOLEAN(byte val) {
        return val != 0; // JNR conversion to boolean is not working correctly, so we need to do it here
    }

    public static boolean BOOL(int val) {
        return val != 0; // JNR conversion to boolean is not working correctly, so we need to do it here
    }
}
