package com.github.jnrwinfspteam.jnrwinfsp.internal.util;

import jnr.ffi.Pointer;
import jnr.ffi.Struct;

public class Pointered<T extends Struct> {
    public static <T extends Struct> Pointered<T> wrap(T obj, Pointer pointer) {
        obj.useMemory(pointer);

        return new Pointered<>(obj, pointer);
    }

    public static <T extends Struct> Pointered<T> allocate(T obj) {
        Pointer pointer = PointerUtils.allocateMemory(obj.getRuntime(), Struct.size(obj));
        obj.useMemory(pointer);

        return new Pointered<>(obj, pointer);
    }

    private final T obj;
    private final Pointer pointer;

    private Pointered(T obj, Pointer pointer) {
        this.obj = obj;
        this.pointer = pointer;
    }

    public T get() {
        return obj;
    }

    public Pointer getPointer() {
        return pointer;
    }

    public void free() {
        PointerUtils.freeMemory(this.pointer);
    }
}