package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;

import java.nio.file.Path;

public class DirObj extends MemoryObj {

    public DirObj(Path path, String securityDescriptor) {
        super(path, securityDescriptor);
        getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    }

    @Override
    protected int getAllocationSize() {
        return 0;
    }

    @Override
    protected int getFileSize() {
        return 0;
    }
}
