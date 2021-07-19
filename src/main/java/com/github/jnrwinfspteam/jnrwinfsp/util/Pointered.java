package com.github.jnrwinfspteam.jnrwinfsp.util;

import com.kenai.jffi.MemoryIO;
import jnr.ffi.Pointer;
import jnr.ffi.Struct;

public class Pointered<T extends Struct> {
    public static <T extends Struct> Pointered<T> wrap(T obj, Pointer pointer) {
        obj.useMemory(pointer);

        return new Pointered<>(obj, pointer);
    }

    public static <T extends Struct> Pointered<T> allocate(T obj) {
        long address = MemoryIO.getInstance().allocateMemory(Struct.size(obj), true);
        Pointer pointer = jnr.ffi.Pointer.wrap(obj.getRuntime(), address);
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
        MemoryIO.getInstance().freeMemory(this.pointer.address());
    }
}