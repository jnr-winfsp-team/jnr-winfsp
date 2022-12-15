package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.util.Objects;

public final class OpenContext {

    public enum Type {
        FILE,
        DIRECTORY
    }

    static OpenContext newFileContext(String path) {
        return new OpenContext(path, Type.FILE);
    }

    static OpenContext newDirectoryContext(String path) {
        return new OpenContext(path, Type.DIRECTORY);
    }

    private volatile String path;
    private volatile Type type;

    private OpenContext(String path, Type type) {
        this.path = Objects.requireNonNull(path);
        this.type = Objects.requireNonNull(type);
    }

    public String getPath() {
        return path;
    }

    public Type getType() {
        return type;
    }

    public boolean isFile() {
        return Type.FILE.equals(type);
    }

    public boolean isDirectory() {
        return Type.DIRECTORY.equals(type);
    }

    public void setPath(String path) {
        this.path = Objects.requireNonNull(path);
    }

    public void setType(Type type) {
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public String toString() {
        return java.lang.String.format("(%s) %s", type, path);
    }
}
