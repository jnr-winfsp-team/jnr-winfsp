package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.api.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.api.ReparsePoint;
import com.github.jnrwinfspteam.jnrwinfsp.util.NaturalOrderComparator;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.TreeMap;

public class DirObj extends MemoryObj {

    private static final Comparator<String> NAME_ORDER = new NaturalOrderComparator();

    /**
     * Children of this directory, pre-sorted in natural order.
     * Maintained by WinFspMemFS.putObject / removeObject.
     */
    private final TreeMap<String, MemoryObj> children = new TreeMap<>(NAME_ORDER);

    public DirObj(Path path, byte[] securityDescriptor, ReparsePoint reparsePoint) {
        super(path, securityDescriptor, reparsePoint);
        getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    }

    public TreeMap<String, MemoryObj> getChildren() {
        return children;
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
