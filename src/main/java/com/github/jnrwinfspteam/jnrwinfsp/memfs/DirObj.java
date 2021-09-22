package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.api.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.api.ReparsePoint;

import java.nio.file.Path;

public class DirObj extends MemoryObj {

    public DirObj(DirObj parent, Path path, byte[] securityDescriptor, ReparsePoint reparsePoint) {
        super(parent, path, securityDescriptor, reparsePoint);
        getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    }

    @Override
    public int getAllocationSize() {
        return 0;
    }

    @Override
    public int getFileSize() {
        return 0;
    }
}
