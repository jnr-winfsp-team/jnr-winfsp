package com.github.jnrwinfspteam.jnrwinfsp.internal.util;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.*;

public final class StringUtils {
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
     * @return A pointer (null if the string is null)
     */
    public static Pointer toPointer(Runtime runtime, String s, boolean nullTerminated) {
        if (s == null)
            return null;

        try {
            byte[] bytes = getEncoder().reset().encode(CharBuffer.wrap(s)).array();
            int finalLength = bytes.length;
            if (nullTerminated)
                finalLength = Math.addExact(finalLength, CS_BYTES_PER_CHAR);

            Pointer p = PointerUtils.allocateMemory(runtime, finalLength);

            p.put(0, bytes, 0, bytes.length);
            if (nullTerminated)
                for (int i = 0; i < CS_BYTES_PER_CHAR; i++)
                    p.putByte(bytes.length, (byte) 0);

            return p;
        } catch (CharacterCodingException cce) {
            throw new RuntimeException(cce);
        }
    }

    public static void freeStringPointer(Pointer pStr) {
        PointerUtils.freeMemory(pStr);
    }

    /**
     * Reads a null-terminated string from a given pointer, using the configured charset for decoding
     * the bytes.
     * <p>
     * This code is adapted from jnr.ffi.provider.converters.StringResultConverter but with a
     * small fix to handle multibyte-alignment issues.
     *
     * @return a string (null if the pointer is null)
     */
    public static String fromPointer(Pointer pStr) {
        if (pStr == null)
            return null;

        Search:
        for (int idx = 0; ; ) {
            idx += pStr.indexOf(idx, (byte) 0);

            // Align idx up to the next char boundary. If indexOf landed on an
            // unaligned zero (e.g. the 0x00 in 0x00 0x03 of U+0300 in UTF-16LE),
            // skip past it and resume searching from the aligned position.
            boolean wasAligned = true;
            while (idx % CS_BYTES_PER_CHAR != 0) {
                idx++;
                wasAligned = false;
            }
            if (!wasAligned)
                continue Search;

            // idx is aligned and points at a zero byte. Check the rest of the char.
            for (int tcount = 1; tcount < CS_BYTES_PER_CHAR; tcount++) {
                byte b = pStr.getByte(idx + tcount);
                if (b != 0) {
                    idx += CS_BYTES_PER_CHAR;
                    continue Search;
                }
            }

            byte[] bytes = new byte[idx];
            pStr.get(0, bytes, 0, bytes.length);
            try {
                return getDecoder().reset().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException cce) {
                throw new RuntimeException(cce);
            }
        }
    }

    public static byte[] toBytes(String s, boolean nullTerminated) {
        if (s == null)
            return null;

        try {
            ByteBuffer buf = getEncoder().reset().encode(CharBuffer.wrap(s));
            if (!nullTerminated)
                return buf.array();

            final int bufSize = buf.remaining();
            byte[] bytes = new byte[bufSize + CS_BYTES_PER_CHAR];
            buf.get(bytes, 0, bufSize);
            for (int i = 0; i < CS_BYTES_PER_CHAR; i++)
                bytes[bufSize + i] = (byte) 0;

            return bytes;
        } catch (CharacterCodingException cce) {
            throw new RuntimeException(cce);
        }
    }

    private StringUtils() {
        // not instantiable
    }
}
