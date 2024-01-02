package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.util.Objects;

public final class OpenContext {

    public enum Type {
        FILE,
        DIRECTORY
    }

    static OpenContext newFileContext(long fileHandle, String path) {
        return new OpenContext(fileHandle, path, Type.FILE);
    }

    static OpenContext newDirectoryContext(long fileHandle, String path) {
        return new OpenContext(fileHandle, path, Type.DIRECTORY);
    }

    private final long fileHandle;
    private volatile String path;
    private volatile Type type;

    private OpenContext(long fileHandle, String path, Type type) {
        this.fileHandle = fileHandle;
        this.path = Objects.requireNonNull(path);
        this.type = Objects.requireNonNull(type);
    }

    public long getFileHandle() {
        return fileHandle;
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
        return String.format("%d - (%s) %s", fileHandle, type, path);
    }
}
