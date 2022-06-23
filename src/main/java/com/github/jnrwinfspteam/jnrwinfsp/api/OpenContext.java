package com.github.jnrwinfspteam.jnrwinfsp.api;

import com.github.jnrwinfspteam.jnrwinfsp.internal.util.Pointered;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.StringUtils;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class OpenContext extends Struct {

    public enum Type {
        FILE,
        DIRECTORY
    }

    public final Struct.Pointer path = new Pointer();
    public final Struct.Enum8<Type> type = new Enum8<>(Type.class);

    public static Pointered<OpenContext> of(jnr.ffi.Pointer pointer) {
        return Pointered.wrap(new OpenContext(Runtime.getSystemRuntime()), pointer);
    }

    public static Pointered<OpenContext> create(Runtime runtime) {
        var ctx = new OpenContext(runtime);

        // allocate the necessary memory for the struct
        Pointered<OpenContext> ctxP = Pointered.allocate(ctx);

        // initialise every member to zero
        ctx.path.set(0L);
        ctx.type.set(0);

        return ctxP;
    }

    private OpenContext(Runtime runtime) {
        super(runtime);
    }

    public void setPath(java.lang.String path) {
        this.path.set(StringUtils.toPointer(this.getRuntime(), path, true));
    }

    public void setType(Type type) {
        this.type.set(type);
    }

    public java.lang.String getPath() {
        return StringUtils.fromPointer(this.path.get());
    }

    public Type getType() {
        return this.type.get();
    }

    public boolean isFile() {
        return Type.FILE.equals(getType());
    }

    public boolean isDirectory() {
        return Type.DIRECTORY.equals(getType());
    }

    @Override
    public java.lang.String toString() {
        return java.lang.String.format("(%s) %s", getType(), getPath());
    }
}
