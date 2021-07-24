package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;
import jnr.ffi.Pointer;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class FileObj extends MemoryObj {
    private static final int ALLOCATION_UNIT = 4096;

    private ByteBuffer data;

    public FileObj(Path path, String securityDescriptor) {
        super(path, securityDescriptor);
        this.data = ByteBuffer.allocate(0).limit(0);
        getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
    }

    @Override
    protected synchronized int getAllocationSize() {
        return data.capacity();
    }

    @Override
    protected synchronized int getFileSize() {
        return data.remaining();
    }

    public synchronized void setFileSize(int fileSize) {
        final int prevFileSize = getFileSize();

        if (fileSize < prevFileSize) {
            for (int i = fileSize; i < prevFileSize; i++) {
                data.put(i, (byte) 0);
            }
            setWriteTimes();
        } else if (fileSize > getAllocationSize())
            adaptAllocationSize(fileSize);

        data.limit(fileSize);
    }

    public synchronized void adaptAllocationSize(int fileSize) {
        int units = (Math.addExact(fileSize, ALLOCATION_UNIT) - 1) / ALLOCATION_UNIT;
        setAllocationSize(units * ALLOCATION_UNIT);
    }

    public synchronized void setAllocationSize(int newAllocationSize) {
        final int newFileSize = Math.min(getFileSize(), newAllocationSize);
        if (newAllocationSize < getAllocationSize()) {
            // truncate the data buffer
            ByteBuffer copy = ByteBuffer.allocate(newAllocationSize);
            data.limit(newAllocationSize);
            copy.put(data);
            copy.rewind();
            copy.limit(newFileSize);
            data = copy;

            setWriteTimes();
        } else if (newAllocationSize > getAllocationSize()) {
            // extend the data buffer
            ByteBuffer copy = ByteBuffer.allocate(newAllocationSize);
            copy.put(data);
            copy.rewind();
            copy.limit(newFileSize);
            data = copy;

            setWriteTimes();
        }
    }

    public synchronized int read(Pointer buffer, long offsetL, int size) throws NTStatusException {
        final int offset = Math.toIntExact(offsetL);
        if (offset > getFileSize())
            throw new NTStatusException(0xC0000011); // STATUS_END_OF_FILE

        int bytesToRead = Math.min(getFileSize() - offset, size);
        byte[] dst = new byte[bytesToRead];
        data.position(offset);
        data.get(dst, 0, bytesToRead);
        buffer.put(0, dst, 0, bytesToRead);
        data.rewind();

        setReadTimes();

        return bytesToRead;
    }

    public synchronized int write(Pointer buffer, long offsetL, int size, boolean writeToEndOfFile) {
        int offset = Math.toIntExact(offsetL);
        if (writeToEndOfFile)
            offset = getFileSize();

        int maxWriteIndex = Math.addExact(offset, size);
        byte[] dst = new byte[size];
        if (maxWriteIndex > getAllocationSize())
            setAllocationSize(maxWriteIndex);

        buffer.get(0, dst, 0, size);
        data.position(offset);
        data.put(dst);
        data.flip();

        setWriteTimes();

        return size;
    }

    public synchronized int constrainedWrite(Pointer buffer, long offsetL, int size) {
        final int offset = Math.toIntExact(offsetL);
        if (offset >= getFileSize())
            return 0;

        int endOffset = Math.min(getFileSize(), Math.addExact(offset, size));
        int transferredLength = endOffset - offset;

        byte[] dst = new byte[transferredLength];
        buffer.get(0, dst, 0, transferredLength);
        data.position(offset);
        data.put(dst);
        data.flip();

        setWriteTimes();

        return transferredLength;
    }

    private void setReadTimes() {
        setAccessTime(WinSysTime.now());
    }

    private void setWriteTimes() {
        WinSysTime now = WinSysTime.now();
        setAccessTime(now);
        setWriteTime(now);
        setChangeTime(now);
    }
}
