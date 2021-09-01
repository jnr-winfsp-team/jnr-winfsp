package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.util.Arrays;
import java.util.Objects;

public class ReparsePoint {

    private final byte[] data;
    private final int tag;

    public ReparsePoint(byte[] data, int tag) {
        this.data = Objects.requireNonNull(data);
        this.tag = tag;
    }

    public byte[] getData() {
        return data;
    }

    public int getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "{" + tag + "; " + Arrays.toString(data) + "}";
    }
}
