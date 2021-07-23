package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.MountException;
import com.github.jnrwinfspteam.jnrwinfsp.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.WinFspStubFS;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.FileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultRead;
import com.github.jnrwinfspteam.jnrwinfsp.result.VolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import jnr.ffi.Pointer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple in-memory file system.
 */
public class WinFspMemFS extends WinFspStubFS {
    public static void main(String[] args) throws MountException, IOException {
        Path mountPoint = null;
        if (args.length > 0)
            mountPoint = Paths.get(args[0]);

        var memFS = new WinFspMemFS();
        System.out.printf("Mounting %s...%n", mountPoint == null ? "" : mountPoint);
        memFS.mountLocalDrive(mountPoint, true);
        try {
            System.out.println("<Press Enter to quit>");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } finally {
            System.out.printf("Unmounting %s...%n", mountPoint == null ? "" : mountPoint);
            memFS.unmountLocalDrive();
            System.out.println("<done>");
        }
    }


    private static final String SECURITY_DESCRIPTOR = "O:BAG:BAD:P(A;;FA;;;SY)(A;;FA;;;BA)(A;;FA;;;WD)";
    private static final long MAX_FILE_NODES = 1024;
    private static final long MAX_FILE_SIZE = 16 * 1024 * 1024;

    private final Map<String, MemoryObj> entries;
    private String volumeLabel;

    public WinFspMemFS() {
        this.entries = new HashMap<>();
        this.entries.put("\\", new DirObj(Path.of("\\"), SECURITY_DESCRIPTOR));
        this.volumeLabel = "MemFS";
    }

    @Override
    public VolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) {
        System.out.println("=== GET VOLUME INFO");
        return generateVolumeInfo();
    }

    @Override
    public VolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) {
        System.out.println("=== SET VOLUME LABEL");
        this.volumeLabel = volumeLabel;
        return generateVolumeInfo();
    }

//    @Override
//    public ResultSecurityAndAttributes getSecurityByName(FSP_FILE_SYSTEM fileSystem, String fileName) {
//        MemoryObj memObj = entries.get(fileName);
//    }

    @Override
    public FileInfo create(FSP_FILE_SYSTEM fileSystem,
                           String fileName,
                           Set<CreateOptions> createOptions,
                           int grantedAccess,
                           Set<FileAttributes> fileAttributes,
                           Pointer pSecurityDescriptor,
                           long allocationSize) throws NTStatusException {

        System.out.println("=== CREATE " + fileName);
        synchronized (entries) {
            Path filePath = Path.of(fileName);

            // Ensure the parent object exists and is a directory
            MemoryObj parentObj = getObject(filePath.getParent());
            if (parentObj instanceof FileObj)
                throw new NTStatusException(0xC0000103); // STATUS_NOT_A_DIRECTORY

            // Check for duplicate file/folder
            if (entries.containsKey(filePath.toString()))
                throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION

            MemoryObj obj;
            if (createOptions.contains(CreateOptions.FILE_DIRECTORY_FILE))
                obj = new DirObj(filePath, SECURITY_DESCRIPTOR);
            else {
                var fileObj = new FileObj(filePath, SECURITY_DESCRIPTOR);
                fileObj.setAllocationSize(Math.toIntExact(allocationSize));
                obj = fileObj;
            }

            entries.put(filePath.toString(), obj);

            return obj.generateFileInfo();
        }
    }

    @Override
    public FileInfo open(FSP_FILE_SYSTEM fileSystem,
                         String fileName,
                         Set<CreateOptions> createOptions,
                         int grantedAccess) throws NTStatusException {

        System.out.println("=== OPEN " + fileName);
        synchronized (entries) {
            Path filePath = Path.of(fileName);
            MemoryObj obj = getObject(filePath);

            return obj.generateFileInfo();
        }
    }

    @Override
    public FileInfo overwrite(FSP_FILE_SYSTEM fileSystem,
                              String fileName,
                              Set<FileAttributes> fileAttributes,
                              boolean replaceFileAttributes,
                              long allocationSize) throws NTStatusException {

        System.out.println("=== OVERWRITE " + fileName);
        synchronized (entries) {
            Path filePath = Path.of(fileName);
            FileObj fileObj = getFileObject(filePath);

            fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
            if (replaceFileAttributes)
                fileObj.getFileAttributes().clear();
            fileObj.getFileAttributes().addAll(fileAttributes);

            fileObj.setAllocationSize(Math.toIntExact(allocationSize));

            return fileObj.generateFileInfo();
        }
    }

    @Override
    public void close(FSP_FILE_SYSTEM fileSystem, String fileName) {
        System.out.println("=== CLOSE " + fileName);
    }

    @Override
    public long read(FSP_FILE_SYSTEM fileSystem,
                     String fileName,
                     Pointer pBuffer,
                     long offset,
                     long length) throws NTStatusException {

        System.out.println("=== READ " + fileName);
        synchronized (entries) {
            Path filePath = Path.of(fileName);
            FileObj fileObj = getFileObject(filePath);

            return fileObj.read(pBuffer, Math.toIntExact(offset), Math.toIntExact(length));
        }
    }

    @Override
    public FileInfo getFileInfo(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        System.out.println("=== GET FILE INFO " + fileName);
        synchronized (entries) {
            Path filePath = Path.of(fileName);
            MemoryObj obj = getObject(filePath);

            return obj.generateFileInfo();
        }
    }

    @Override
    public ResultRead readDirectory(FSP_FILE_SYSTEM fileSystem,
                                    String fileName,
                                    String pattern,
                                    String marker,
                                    Pointer pBuffer,
                                    long length) {
        System.out.println("=== READ DIRECTORY " + fileName);
        return new ResultRead(0);
    }

    private MemoryObj getObject(Path filePath) throws NTStatusException {
        MemoryObj obj = entries.get(filePath.toString());
        if (obj == null)
            throw new NTStatusException(0xC0000034); // STATUS_OBJECT_NAME_NOT_FOUND

        return obj;
    }

    private FileObj getFileObject(Path filePath) throws NTStatusException {
        MemoryObj obj = getObject(filePath);
        if (!(obj instanceof FileObj))
            throw new NTStatusException(0xC000000D); // STATUS_INVALID_PARAMETER

        return (FileObj) obj;
    }

    private VolumeInfo generateVolumeInfo() {
        return new VolumeInfo(
                MAX_FILE_NODES * MAX_FILE_SIZE,
                (MAX_FILE_NODES - entries.size()) * MAX_FILE_SIZE,
                this.volumeLabel
        );
    }
}
