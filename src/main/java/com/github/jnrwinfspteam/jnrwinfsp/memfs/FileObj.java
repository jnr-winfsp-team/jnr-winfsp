package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.api.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.api.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.api.WinSysTime;
import jnr.ffi.Pointer;

import java.nio.file.Path;
import java.util.Arrays;

public class FileObj extends MemoryObj {
    private static final int ALLOCATION_UNIT = 512;

    private byte[] data;
    private int fileSize;

    public FileObj(DirObj parent, Path path, String securityDescriptor, byte[] reparseData, int reparseTag) {
        super(parent, path, securityDescriptor, reparseData, reparseTag);
        this.data = new byte[0];
        this.fileSize = 0;
        getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
    }

    @Override
    public synchronized int getAllocationSize() {
        return data.length;
    }

    @Override
    public synchronized int getFileSize() {
        return fileSize;
    }

    public synchronized void setFileSize(int fileSize) {
        final int prevFileSize = getFileSize();

        if (fileSize < prevFileSize) {
            for (int i = fileSize; i < prevFileSize; i++) {
                data[i] = (byte) 0;
            }
        } else if (fileSize > getAllocationSize())
            adaptAllocationSize(fileSize);

        this.fileSize = fileSize;
    }

    public synchronized void adaptAllocationSize(int fileSize) {
        int units = (Math.addExact(fileSize, ALLOCATION_UNIT) - 1) / ALLOCATION_UNIT;
        setAllocationSize(units * ALLOCATION_UNIT);
    }

    public synchronized void setAllocationSize(int newAllocationSize) {
        if (newAllocationSize != getAllocationSize()) {
            // truncate or extend the data buffer
            final int newFileSize = Math.min(getFileSize(), newAllocationSize);
            this.data = Arrays.copyOf(data, newAllocationSize);
            this.fileSize = newFileSize;
        }
    }

    public synchronized int read(Pointer buffer, long offsetL, int size) throws NTStatusException {
        int offset = Math.toIntExact(offsetL);
        if (offset >= getFileSize())
            throw new NTStatusException(0xC0000011); // STATUS_END_OF_FILE

        int bytesToRead = Math.min(getFileSize() - offset, size);
        buffer.put(0, data, offset, bytesToRead);

        setReadTime();

        return bytesToRead;
    }

    public synchronized int write(Pointer buffer, long offsetL, int size, boolean writeToEndOfFile) {
        int begOffset = Math.toIntExact(offsetL);
        if (writeToEndOfFile)
            begOffset = getFileSize();

        int endOffset = Math.addExact(begOffset, size);
        if (endOffset > getFileSize())
            setFileSize(endOffset);

        buffer.get(0, data, begOffset, size);

        setWriteTime();

        return size;
    }

    public synchronized int constrainedWrite(Pointer buffer, long offsetL, int size) {
        int begOffset = Math.toIntExact(offsetL);
        if (begOffset >= getFileSize())
            return 0;

        int endOffset = Math.min(getFileSize(), Math.addExact(begOffset, size));
        int transferredLength = endOffset - begOffset;

        buffer.get(0, data, begOffset, transferredLength);

        setWriteTime();

        return transferredLength;
    }

    private void setReadTime() {
        setAccessTime(WinSysTime.now());
    }

    private void setWriteTime() {
        setWriteTime(WinSysTime.now());
    }
}
