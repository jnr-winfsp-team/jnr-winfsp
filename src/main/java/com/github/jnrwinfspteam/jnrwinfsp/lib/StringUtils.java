package com.github.jnrwinfspteam.jnrwinfsp.lib;

import com.kenai.jffi.MemoryIO;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.*;

public class StringUtils {
    public static final int CS_BYTES_PER_CHAR = 2;
    public static final Charset CS = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
            ? StandardCharsets.UTF_16LE
            : StandardCharsets.UTF_16BE;

    private static final ThreadLocal<CharsetEncoder> cachedEncoder = ThreadLocal.withInitial(() -> CS.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
    );
    private static final ThreadLocal<CharsetDecoder> cachedDecoder = ThreadLocal.withInitial(() -> CS.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
    );

    /**
     * Returns a cached per-thread charset encoder.
     *
     * @return a reusable CharsetEncoder
     */
    public static CharsetEncoder getEncoder() {
        return cachedEncoder.get();
    }

    /**
     * Returns a cached per-thread charset decoder.
     *
     * @return a reusable CharsetDecoder
     */
    public static CharsetDecoder getDecoder() {
        return cachedDecoder.get();
    }

    /**
     * Returns a pointer containing the contents of the given string, encoded with the configured charset.
     *
     * @param s A string
     * @return A pointer (null if the string is null)
     */
    public static Pointer toPointer(Runtime runtime, String s) {
        if (s == null)
            return null;

        try {
            byte[] bytes = getEncoder().reset().encode(CharBuffer.wrap(s)).array();

            long address = MemoryIO.getInstance().allocateMemory(bytes.length + 1, true);
            Pointer p = Pointer.wrap(runtime, address,bytes.length + 1);
            p.put(0, bytes, 0, bytes.length);
            p.putByte(bytes.length, (byte) 0);
            return p;
        } catch (CharacterCodingException cce) {
            throw new RuntimeException(cce);
        }
    }

    public static void freeStringPointer(Pointer pStr) {
        MemoryIO.getInstance().freeMemory(pStr.address());
    }

    /**
     * Reads a null-terminated string from a given pointer, using the configured charset for decoding
     * the bytes.
     * <p>
     * This code is adapted from jnr.ffi.provider.converters.StringResultConverter but with a
     * small fix to handle the case where a null terminator character is shorter than the
     * configured terminator length (2 in the case of UTF-16{LE|BE})
     *
     * @param pStr A pointer to a string
     * @return a string (null if the pointer is null)
     */
    public static String fromPointer(Pointer pStr) {
        if (pStr == null)
            return null;

        Search:
        for (int idx = 0; ; ) {
            idx += pStr.indexOf(idx, (byte) 0);
            for (int tcount = 1; tcount < CS_BYTES_PER_CHAR; tcount++) {
                byte b = pStr.getByte(idx + tcount);
                if (b != 0) {
                    idx += tcount;
                    continue Search;
                }
            }

            // Small fix here to accommodate enough bytes for the string.
            // NOTE: this WILL NOT make the string include a null character
            while (idx % CS_BYTES_PER_CHAR != 0)
                idx++;

            byte[] bytes = new byte[idx];
            pStr.get(0, bytes, 0, bytes.length);
            try {
                return getDecoder().reset().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException cce) {
                throw new RuntimeException(cce);
            }
        }
    }
}
