package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.MountException;
import com.github.jnrwinfspteam.jnrwinfsp.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.WinFspStubFS;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CleanupFlags;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.FileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.result.VolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;
import jnr.ffi.Pointer;

import java.io.*;
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
        memFS.mountLocalDrive(mountPoint, false);
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

    private final PrintStream verboseOut;

    public WinFspMemFS() {
        this(false);
    }

    public WinFspMemFS(boolean verbose) {
        this.rootPath = Path.of("\\").normalize();
        this.objects = new HashMap<>();
        this.objects.put(rootPath.toString(), new DirObj(rootPath, SECURITY_DESCRIPTOR));
//      this.objects.put("\\TestDir", new DirObj(Path.of("\\TestDir").normalize(), SECURITY_DESCRIPTOR));
//      this.objects.put("\\TestFile", new FileObj(Path.of("\\TestFile").normalize(), SECURITY_DESCRIPTOR));
        this.volumeLabel = "MemFS";

        this.verboseOut = verbose ? System.out : new PrintStream(OutputStream.nullOutputStream());
    }

    @Override
    public VolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) {

        verboseOut.println("=== GET VOLUME INFO");
        return generateVolumeInfo();
    }

    @Override
    public VolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) {

        verboseOut.println("=== SET VOLUME LABEL");
        this.volumeLabel = volumeLabel;
        return generateVolumeInfo();
    }

    @Override
    public FileInfo create(FSP_FILE_SYSTEM fileSystem,
                           String fileName,
                           Set<CreateOptions> createOptions,
                           int grantedAccess,
                           Set<FileAttributes> fileAttributes,
                           String securityDescriptorString,
                           long allocationSize) throws NTStatusException {

        verboseOut.println("=== CREATE " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);

            // Ensure the parent object exists and is a directory
            getDirObject(filePath.getParent());

            // Check for duplicate file/folder
            if (hasObject(filePath))
                throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION

            MemoryObj obj;
            if (createOptions.contains(CreateOptions.FILE_DIRECTORY_FILE))
                obj = new DirObj(filePath, securityDescriptorString);
            else {
                var file = new FileObj(filePath, securityDescriptorString);
                file.setAllocationSize(Math.toIntExact(allocationSize));
                obj = file;
            }

            obj.getFileAttributes().addAll(fileAttributes);
            putObject(obj);

            return obj.generateFileInfo();
        }
    }

    @Override
    public FileInfo open(FSP_FILE_SYSTEM fileSystem,
                         String fileName,
                         Set<CreateOptions> createOptions,
                         int grantedAccess) throws NTStatusException {

        verboseOut.println("=== OPEN " + fileName);
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

        verboseOut.println("=== OVERWRITE " + fileName);
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
    public void cleanup(FSP_FILE_SYSTEM fileSystem, String fileName, Set<CleanupFlags> flags) {

        verboseOut.println("=== CLEANUP " + fileName);
        try {
            synchronized (objects) {
                Path filePath = getPath(fileName);
                MemoryObj memObj = getObject(filePath);

                if (flags.contains(CleanupFlags.DELETE)) {
                    if (isNotEmptyDirectory(memObj))
                        return; // abort if trying to remove a non-empty directory
                    removeObject(memObj.getPath());
                }

                if (flags.contains(CleanupFlags.SET_ALLOCATION_SIZE) && memObj instanceof FileObj)
                    ((FileObj) memObj).adaptAllocationSize(memObj.getFileSize());

                if (flags.contains(CleanupFlags.SET_ARCHIVE_BIT) && memObj instanceof FileObj)
                    memObj.getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);

                if (flags.contains(CleanupFlags.SET_LAST_ACCESS_TIME))
                    memObj.setAccessTime(WinSysTime.now());

                if (flags.contains(CleanupFlags.SET_LAST_WRITE_TIME))
                    memObj.setWriteTime(WinSysTime.now());

                if (flags.contains(CleanupFlags.SET_CHANGE_TIME))
                    memObj.setChangeTime(WinSysTime.now());

            }
        } catch (NTStatusException e) {
            // we have no way to pass an error status via cleanup
        }
    }

    @Override
    public void close(FSP_FILE_SYSTEM fileSystem, String fileName) {
        verboseOut.println("=== CLOSE " + fileName);
    }

    @Override
    public long read(FSP_FILE_SYSTEM fileSystem,
                     String fileName,
                     Pointer pBuffer,
                     long offset,
                     int length) throws NTStatusException {

        verboseOut.println("=== READ " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            return file.read(pBuffer, offset, length);
        }
    }

    @Override
    public long write(FSP_FILE_SYSTEM fileSystem,
                      String fileName,
                      Pointer pBuffer,
                      long offset,
                      int length,
                      boolean writeToEndOfFile,
                      boolean constrainedIo) throws NTStatusException {

        verboseOut.println("=== WRITE " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

//            if (constrainedIo)
//                return file.constrainedWrite(pBuffer, offset, length);
//            else
            return file.write(pBuffer, offset, length, writeToEndOfFile);
        }
    }

    @Override
    public void flush(FSP_FILE_SYSTEM fileSystem, String fileName) {
        verboseOut.println("=== FLUSH " + fileName);
    }

    @Override
    public FileInfo getFileInfo(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

        verboseOut.println("=== GET FILE INFO " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj obj = getObject(filePath);

            return obj.generateFileInfo();
        }
    }

    @Override
    public FileInfo setBasicInfo(FSP_FILE_SYSTEM fileSystem,
                                 String fileName,
                                 Set<FileAttributes> fileAttributes,
                                 WinSysTime creationTime,
                                 WinSysTime lastAccessTime,
                                 WinSysTime lastWriteTime,
                                 WinSysTime changeTime) throws NTStatusException {

        verboseOut.println("=== SET BASIC INFO " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj obj = getObject(filePath);

            if (!fileAttributes.contains(FileAttributes.INVALID_FILE_ATTRIBUTES)) {
                obj.getFileAttributes().clear();
                obj.getFileAttributes().addAll(fileAttributes);
            }
            if (creationTime.get() != 0)
                obj.setCreationTime(creationTime);
            if (lastAccessTime.get() != 0)
                obj.setAccessTime(lastAccessTime);
            if (lastWriteTime.get() != 0)
                obj.setWriteTime(lastWriteTime);
            if (changeTime.get() != 0)
                obj.setWriteTime(changeTime);

            return obj.generateFileInfo();
        }
    }

    @Override
    public FileInfo setFileSize(FSP_FILE_SYSTEM fileSystem, String fileName, long newSize, boolean setAllocationSize)
            throws NTStatusException {

        verboseOut.println("=== SET FILE SIZE " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            if (setAllocationSize)
                file.setAllocationSize(Math.toIntExact(newSize));
            else
                file.setFileSize(Math.toIntExact(newSize));

            return file.generateFileInfo();
        }
    }

    @Override
    public void canDelete(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

        verboseOut.println("=== CAN DELETE " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            if (isNotEmptyDirectory(memObj))
                throw new NTStatusException(0xC0000101); // STATUS_DIRECTORY_NOT_EMPTY
        }
    }

    @Override
    public void rename(FSP_FILE_SYSTEM fileSystem, String fileName, String newFileName, boolean replaceIfExists)
            throws NTStatusException {

        verboseOut.println("=== RENAME " + fileName + " -> " + newFileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            Path newFilePath = getPath(newFileName);

            MemoryObj memObj = getObject(filePath);
            // Handle existing new file/directory scenario
            if (hasObject(newFilePath)) {
                // case-sensitive comparison
                if (Objects.equals(
                        Objects.toString(Path.of(newFileName).normalize().getFileName(), null),
                        memObj.getName())
                ) {
                    if (!replaceIfExists)
                        throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION
                    else if (memObj instanceof DirObj)
                        // a directory cannot be renamed to one that already exists
                        throw new NTStatusException(0xC0000022); // STATUS_ACCESS_DENIED
                }
            }

            // Rename file or directory (and all existing children)
            for (var obj : List.copyOf(objects.values())) {
                if (obj.getPath().startsWith(filePath)) {
                    Path relativePath = obj.getPath().relativize(filePath);
                    Path newObjPath = newFilePath.resolve(relativePath);
                    MemoryObj newObj = removeObject(obj.getPath());
                    newObj.setPath(newObjPath);
                    putObject(newObj);
                }
            }
        }
    }

    @Override
    public String getSecurity(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

        verboseOut.println("=== GET SECURITY " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            return memObj.getSecurityDescriptor();
        }
    }

    @Override
    public void setSecurity(FSP_FILE_SYSTEM fileSystem, String fileName, String securityDescriptorStr)
            throws NTStatusException {

        verboseOut.println("=== SET SECURITY " + fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);
            memObj.setSecurityDescriptor(securityDescriptorStr);
        }
    }

    @Override
    public List<FileInfo> readDirectory(FSP_FILE_SYSTEM fileSystem, String fileName, String pattern, String marker)
            throws NTStatusException {

        verboseOut.println("=== READ DIRECTORY " + fileName);
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

    private boolean isNotEmptyDirectory(MemoryObj dir) {
        if (dir instanceof DirObj) {
            for (var obj : objects.values()) {
                if (obj.getPath().startsWith(dir.getPath())
                        && !obj.getPath().equals(dir.getPath()))
                    return true;
            }
        }

        return false;
    }

    private Path getPath(String filePath) {
        return Path.of(filePath).normalize();
    }

    private String getPathKey(Path filePath) {
        if (filePath == null)
            return null;
        else
            return filePath.toString().toLowerCase(Locale.ROOT);
    }

    private boolean hasObject(Path filePath) {
        return objects.containsKey(getPathKey(filePath));
    }

    private MemoryObj getObject(Path filePath) throws NTStatusException {
        MemoryObj obj = objects.get(getPathKey(filePath));
        if (obj == null)
            throw new NTStatusException(0xC0000034); // STATUS_OBJECT_NAME_NOT_FOUND

        return obj;
    }

    private void putObject(MemoryObj obj) {
        objects.put(getPathKey(obj.getPath()), obj);
    }

    private MemoryObj removeObject(Path filePath) {
        return objects.remove(getPathKey(filePath));
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
