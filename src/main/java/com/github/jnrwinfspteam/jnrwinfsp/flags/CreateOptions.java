package com.github.jnrwinfspteam.jnrwinfsp.flags;

import jnr.ffi.util.EnumMapper;

import java.util.EnumSet;
import java.util.Set;

public enum CreateOptions implements EnumMapper.IntegerEnum {

    FILE_DIRECTORY_FILE(0x00000001),
    FILE_WRITE_THROUGH(0x00000002),
    FILE_SEQUENTIAL_ONLY(0x00000004),
    FILE_NO_INTERMEDIATE_BUFFERING(0x00000008),
    FILE_SYNCHRONOUS_IO_ALERT(0x00000010),
    FILE_SYNCHRONOUS_IO_NONALERT(0x00000020),
    FILE_NON_DIRECTORY_FILE(0x00000040),
    FILE_CREATE_TREE_CONNECTION(0x00000080),
    FILE_COMPLETE_IF_OPLOCKED(0x00000100),
    FILE_NO_EA_KNOWLEDGE(0x00000200),
    FILE_OPEN_FOR_RECOVERY(0x00000400),
    FILE_RANDOM_ACCESS(0x00000800),
    FILE_DELETE_ON_CLOSE(0x00001000),
    FILE_OPEN_BY_FILE_ID(0x00002000),
    FILE_OPEN_FOR_BACKUP_INTENT(0x00004000),
    FILE_RESERVE_OPFILTER(0x00100000);

    private static final Set<CreateOptions> ALL_VALUES = EnumSet.allOf(CreateOptions.class);

    public static int intValueOf(Set<CreateOptions> options) {
        int intValue = 0;
        for (var o : options) {
            intValue |= o.intValue();
        }

        return intValue;
    }

    public static EnumSet<CreateOptions> setValueOf(int options) {
        EnumSet<CreateOptions> set = EnumSet.noneOf(CreateOptions.class);
        for (var v : ALL_VALUES) {
            if ((options & v.intValue()) != 0)
                set.add(v);
        }

        return set;
    }

    private final int value;

    CreateOptions(int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }
}
