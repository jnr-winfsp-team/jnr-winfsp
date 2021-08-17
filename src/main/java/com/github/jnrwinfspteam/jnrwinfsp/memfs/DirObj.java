package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;

import java.nio.file.Path;

public class DirObj extends MemoryObj {

    public DirObj(DirObj parent, Path path, String securityDescriptor, byte[] reparseData, int reparseTag) {
        super(parent, path, securityDescriptor, reparseData, reparseTag);
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
