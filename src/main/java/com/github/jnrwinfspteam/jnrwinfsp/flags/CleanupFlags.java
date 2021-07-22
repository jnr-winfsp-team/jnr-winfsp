package com.github.jnrwinfspteam.jnrwinfsp.flags;

import jnr.ffi.util.EnumMapper;

import java.util.EnumSet;
import java.util.Set;

public enum CleanupFlags implements EnumMapper.IntegerEnum {

    DELETE(0x01),
    SET_ALLOCATION_SIZE(0x02),
    SET_ARCHIVE_BIT(0x10),
    SET_LAST_ACCESS_TIME(0x20),
    SET_LAST_WRITE_TIME(0x40),
    SET_CHANGE_TIME(0x80);

    private static final Set<CleanupFlags> ALL_VALUES = EnumSet.allOf(CleanupFlags.class);

    public static int intValueOf(Set<CleanupFlags> flags) {
        int intValue = 0;
        for (var f : flags) {
            intValue |= f.intValue();
        }

        return intValue;
    }

    public static EnumSet<CleanupFlags> setValueOf(int flags) {
        EnumSet<CleanupFlags> set = EnumSet.noneOf(CleanupFlags.class);
        for (var v : ALL_VALUES) {
            if ((flags & v.intValue()) != 0)
                set.add(v);
        }

        return set;
    }

    private final int value;

    CleanupFlags(int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }
}
