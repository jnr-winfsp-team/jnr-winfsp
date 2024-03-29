package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.time.Instant;

public class WinSysTime {

    /**
     * NT time epoch of midnight 1 January 1601.
     */
    public static final Instant ZERO = Instant.parse("1601-01-01T00:00:00Z");

    // (10^-7)s intervals between midnight 1 January 1601 (NT time epoch) and midnight 1 January 1970 (UNIX time epoch)
    private static final long TENTH_MICROS_BETWEEN_EPOCHS = 116444736000000000L;

    /**
     * Returns a new WinSysTime object with the current time.
     *
     * @return a new WinSysTime object
     */
    public static WinSysTime now() {
        return fromInstant(Instant.now());
    }

    /**
     * Constructs a new WinSysTime object from an Instant object.
     *
     * @param instant A time instant
     * @return a new WinSysTime object
     */
    public static WinSysTime fromInstant(Instant instant) {
        // Duration duration = Duration.between(ZERO, instant);
        // long fileTime = (duration.getSeconds() * 10_000_000) + (duration.getNano() / 100);

        long fileTime = TENTH_MICROS_BETWEEN_EPOCHS
                + (instant.getEpochSecond() * 10_000_000)
                + (instant.getNano() / 100);
        return new WinSysTime(fileTime);
    }

    // Windows NT system time in (10^-7)s intervals from midnight 1 January 1601
    private final long fileTime;

    /**
     * Constructs a new WinSysTime object from a Windows NT system time value.
     *
     * @param fileTime Windows NT system time in (10^-7)s intervals from midnight 1 January 1601
     */
    public WinSysTime(long fileTime) {
        this.fileTime = fileTime;
    }

    /**
     * Returns this object's Windows NT system time value.
     *
     * @return a Windows NT system time in (10^-7)s intervals from midnight 1 January 1601
     */
    public final long get() {
        return fileTime;
    }

    /**
     * Converts this WinSysTime to a time instant.
     *
     * @return a time instant representing this WinSysTime object
     */
    public final Instant toInstant() {
        // Duration duration = Duration.of(fileTime / 10, ChronoUnit.MICROS)
        //         .plus(fileTime % 10 * 100, ChronoUnit.NANOS);
        //
        // return ZERO.plus(duration);

        long unixTenthMicros = fileTime - TENTH_MICROS_BETWEEN_EPOCHS;
        long unixSeconds = unixTenthMicros / 10_000_000;
        int unixNanos = (int)((unixTenthMicros % 10_000_000) * 100);

        return Instant.ofEpochSecond(unixSeconds, unixNanos);
    }

    @Override
    public String toString() {
        return toInstant().toString();
    }
}
