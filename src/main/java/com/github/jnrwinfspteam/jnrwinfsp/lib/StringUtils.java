package com.github.jnrwinfspteam.jnrwinfsp.lib;

import java.nio.ByteOrder;
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
}
