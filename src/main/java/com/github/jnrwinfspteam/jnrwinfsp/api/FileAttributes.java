package com.github.jnrwinfspteam.jnrwinfsp.api;

import jnr.ffi.util.EnumMapper;

import java.util.EnumSet;
import java.util.Set;

public enum FileAttributes implements EnumMapper.IntegerEnum {
    FILE_ATTRIBUTE_ARCHIVE(0x20),
    FILE_ATTRIBUTE_COMPRESSED(0x800),
    FILE_ATTRIBUTE_DEVICE(0x40),
    FILE_ATTRIBUTE_DIRECTORY(0x10),
    FILE_ATTRIBUTE_ENCRYPTED(0x4000),
    FILE_ATTRIBUTE_HIDDEN(0x2),
    FILE_ATTRIBUTE_INTEGRITY_STREAM(0x8000),
    FILE_ATTRIBUTE_NORMAL(0x80),
    FILE_ATTRIBUTE_NOT_CONTENT_INDEXED(0x2000),
    FILE_ATTRIBUTE_NO_SCRUB_DATA(0x20000),
    FILE_ATTRIBUTE_OFFLINE(0x1000),
    FILE_ATTRIBUTE_READONLY(0x1),
    FILE_ATTRIBUTE_RECALL_ON_DATA_ACCESS(0x400000),
    FILE_ATTRIBUTE_RECALL_ON_OPEN(0x40000),
    FILE_ATTRIBUTE_REPARSE_POINT(0x400),
    FILE_ATTRIBUTE_SPARSE_FILE(0x200),
    FILE_ATTRIBUTE_SYSTEM(0x4),
    FILE_ATTRIBUTE_TEMPORARY(0x100),
    FILE_ATTRIBUTE_VIRTUAL(0x10000),

    INVALID_FILE_ATTRIBUTES(-1);

    private static final Set<FileAttributes> ALL_VALUES = EnumSet.allOf(FileAttributes.class);

    public static int intOf(Set<FileAttributes> attributes) {
        int intValue = 0;
        for (var a : attributes) {
            intValue |= a.intValue();
        }

        return intValue;
    }

    public static EnumSet<FileAttributes> setOf(int attributes) {
        EnumSet<FileAttributes> set = EnumSet.noneOf(FileAttributes.class);
        for (var v : ALL_VALUES) {
            if ((attributes & v.intValue()) == v.intValue())
                set.add(v);
        }

        return set;
    }

    private final int value;

    FileAttributes(int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }
}
