package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.MountException;
import com.github.jnrwinfspteam.jnrwinfsp.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.WinFspStubFS;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.FileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.result.VolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import jnr.ffi.Pointer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

    private final Path rootPath;
    private String volumeLabel;
    private final Map<String, MemoryObj> objects;

    public WinFspMemFS() {
        this.rootPath = Path.of("\\").normalize();
        this.objects = new HashMap<>();
        this.objects.put(rootPath.toString(), new DirObj(rootPath, SECURITY_DESCRIPTOR));
//      this.objects.put("\\TestDir", new DirObj(Path.of("\\TestDir").normalize(), SECURITY_DESCRIPTOR));
//      this.objects.put("\\TestFile", new FileObj(Path.of("\\TestFile").normalize(), SECURITY_DESCRIPTOR));
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
        synchronized (objects) {
            Path filePath = getPath(fileName);

            // Ensure the parent object exists and is a directory
            getDirObject(filePath.getParent());

            // Check for duplicate file/folder
            if (hasObject(filePath))
                throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION

            MemoryObj obj;
            if (createOptions.contains(CreateOptions.FILE_DIRECTORY_FILE))
                obj = new DirObj(filePath, SECURITY_DESCRIPTOR);
            else {
                var file = new FileObj(filePath, SECURITY_DESCRIPTOR);
                file.setAllocationSize(Math.toIntExact(allocationSize));
                obj = file;
            }

            objects.put(filePath.toString(), obj);

            return obj.generateFileInfo();
        }
    }

    @Override
    public FileInfo open(FSP_FILE_SYSTEM fileSystem,
                         String fileName,
                         Set<CreateOptions> createOptions,
                         int grantedAccess) throws NTStatusException {

        System.out.println("=== OPEN " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
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
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
            if (replaceFileAttributes)
                file.getFileAttributes().clear();
            file.getFileAttributes().addAll(fileAttributes);

            file.setAllocationSize(Math.toIntExact(allocationSize));

            return file.generateFileInfo();
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
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            return file.read(pBuffer, Math.toIntExact(offset), Math.toIntExact(length));
        }
    }

    @Override
    public FileInfo getFileInfo(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        System.out.println("=== GET FILE INFO " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj obj = getObject(filePath);

            return obj.generateFileInfo();
        }
    }

    @Override
    public List<FileInfo> readDirectory(FSP_FILE_SYSTEM fileSystem, String fileName, String pattern, String marker)
            throws NTStatusException {

        System.out.println("=== READ DIRECTORY " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            DirObj dir = getDirObject(filePath);
            var entries = new ArrayList<FileInfo>();

            // only add the "." and ".." entries if the directory is not root
            if (!dir.getPath().equals(rootPath)) {
                DirObj parentDir = getDirObject(filePath.getParent());
                entries.add(dir.generateFileInfo("."));
                entries.add(parentDir.generateFileInfo(".."));
            }

            // include only direct children with relativised names
            for (var obj : objects.values()) {
                Path parent = obj.getPath().getParent();
                if (parent != null
                        && parent.equals(dir.getPath())
                        && !obj.getPath().equals(dir.getPath())) {
                    entries.add(obj.generateFileInfo(obj.getName()));
                }
            }

            // sort the entries by file name
            entries.sort(Comparator.comparing(FileInfo::getFileName));

            // filter out all results before the marker, if it's set
            if (marker != null) {
                return entries.stream()
                        .dropWhile(e -> e.getFileName().compareTo(marker) <= 0)
                        .collect(Collectors.toList());
            }

            return entries;
        }
    }

    private Path getPath(String filePath) {
        return Path.of(filePath).normalize();
    }

    private boolean hasObject(Path filePath) {
        return objects.containsKey(String.valueOf(filePath));
    }

    private MemoryObj getObject(Path filePath) throws NTStatusException {
        MemoryObj obj = objects.get(String.valueOf(filePath));
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

    private DirObj getDirObject(Path filePath) throws NTStatusException {
        MemoryObj obj = getObject(filePath);
        if (!(obj instanceof DirObj))
            throw new NTStatusException(0xC0000103); // STATUS_NOT_A_DIRECTORY

        return (DirObj) obj;
    }

    private VolumeInfo generateVolumeInfo() {
        return new VolumeInfo(
                MAX_FILE_NODES * MAX_FILE_SIZE,
                (MAX_FILE_NODES - objects.size()) * MAX_FILE_SIZE,
                this.volumeLabel
        );
    }
}