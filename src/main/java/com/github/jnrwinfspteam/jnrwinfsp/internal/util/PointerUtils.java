package com.github.jnrwinfspteam.jnrwinfsp.internal.util;

import com.kenai.jffi.MemoryIO;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

public class PointerUtils {

    public static Pointer allocateMemory(Runtime runtime, int size) {
        long address = MemoryIO.getInstance().allocateMemory(size, true);
        return Pointer.wrap(runtime, address, size);
    }

    public static void freeMemory(Pointer p) {
        MemoryIO.getInstance().freeMemory(p.address());
    }

    public static byte[] getBytes(Pointer pBuf, long offset, int size) {
        byte[] bytes = new byte[size];
        pBuf.get(offset, bytes, 0, bytes.length);
        return bytes;
    }

    public static Pointer fromBytes(Runtime runtime, byte[] bytes) {
        return fromBytes(runtime, bytes, 0, bytes.length);
    }

    public static Pointer fromBytes(Runtime runtime, byte[] bytes, int offset, int length) {
        if (bytes == null)
            return null;

        Pointer pBytes = allocateMemory(runtime, length);
        pBytes.put(0, bytes, offset, length);
        return pBytes;
    }

    public static void freeBytesPointer(Pointer pBytes) {
        freeMemory(pBytes);
    }

    public static boolean BOOLEAN(byte val) {
        return val != 0; // JNR conversion to boolean is not working correctly, so we need to do it here
    }

    public static boolean BOOL(int val) {
        return val != 0; // JNR conversion to boolean is not working correctly, so we need to do it here
    }
}
