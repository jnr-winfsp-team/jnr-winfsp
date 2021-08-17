package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.MountException;
import com.github.jnrwinfspteam.jnrwinfsp.NTStatusException;
import com.github.jnrwinfspteam.jnrwinfsp.WinFspStubFS;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CleanupFlags;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.FileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.result.SecurityResult;
import com.github.jnrwinfspteam.jnrwinfsp.result.VolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.result.WriteResult;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import com.github.jnrwinfspteam.jnrwinfsp.util.NaturalOrderComparator;
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
        System.out.printf("Mounting %s ...%n", mountPoint == null ? "" : mountPoint);
        memFS.mountLocalDrive(mountPoint, FSCaseOption.CASE_SENSITIVE, false);
        System.out.println("Mounted");
        try {
            System.out.println("<Press Enter to quit>");
            try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
                reader.readLine();
            }
//            memFS.objects.keySet().stream()
//                    .sorted(NATURAL_ORDER)
//                    .forEachOrdered(System.out::println);
        } finally {
            System.out.printf("Unmounting %s ...%n", mountPoint == null ? "" : mountPoint);
            memFS.unmountLocalDrive();
            System.out.println("<done>");
        }
    }


    private static final String SECURITY_DESCRIPTOR = "O:BAG:BAD:PAR(A;OICI;FA;;;SY)(A;OICI;FA;;;BA)(A;OICI;FA;;;WD)";
    private static final Comparator<String> NATURAL_ORDER = new NaturalOrderComparator();
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
        this.objects.put(rootPath.toString(), new DirObj(null, rootPath, SECURITY_DESCRIPTOR));
        this.volumeLabel = "MemFS";

        this.verboseOut = verbose ? System.out : new PrintStream(OutputStream.nullOutputStream());
    }

    @Override
    public VolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) {

        verboseOut.println("== GET VOLUME INFO ==");
        synchronized (objects) {
            return generateVolumeInfo();
        }
    }

    @Override
    public VolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) {

        verboseOut.printf("== SET VOLUME LABEL == %s%n", volumeLabel);
        synchronized (objects) {
            this.volumeLabel = volumeLabel;
            return generateVolumeInfo();
        }
    }

    @Override
    public SecurityResult getSecurityByName(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        verboseOut.printf("== GET SECURITY BY NAME == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj obj = getObject(filePath);

            String securityDescriptor = obj.getSecurityDescriptor();
            FileInfo info = obj.generateFileInfo();
            verboseOut.printf("== GET SECURITY BY NAME RETURNED == %s %s%n", securityDescriptor, info);

            return new SecurityResult(securityDescriptor, info);
        }
    }

    @Override
    public FileInfo create(FSP_FILE_SYSTEM fileSystem,
                           String fileName,
                           Set<CreateOptions> createOptions,
                           int grantedAccess,
                           Set<FileAttributes> fileAttributes,
                           String securityDescriptor,
                           long allocationSize) throws NTStatusException {

        verboseOut.printf("== CREATE == %s co=%s ga=%X fa=%s sd=%s as=%d%n",
                fileName, createOptions, grantedAccess, fileAttributes, securityDescriptor, allocationSize
        );
        synchronized (objects) {
            Path filePath = getPath(fileName);

            // Check for duplicate file/folder
            if (hasObject(filePath))
                throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION

            // Ensure the parent object exists and is a directory
            DirObj parent = getParentObject(filePath);

            if (objects.size() >= MAX_FILE_NODES)
                throw new NTStatusException(0xC00002EA); // STATUS_CANNOT_MAKE
            if (allocationSize > MAX_FILE_SIZE)
                throw new NTStatusException(0xC000007F); // STATUS_DISK_FULL

            MemoryObj obj;
            if (createOptions.contains(CreateOptions.FILE_DIRECTORY_FILE))
                obj = new DirObj(parent, filePath, securityDescriptor);
            else {
                var file = new FileObj(parent, filePath, securityDescriptor);
                file.setAllocationSize(Math.toIntExact(allocationSize));
                obj = file;
            }

            obj.getFileAttributes().addAll(fileAttributes);
            putObject(obj);

            FileInfo info = obj.generateFileInfo();
            verboseOut.printf("== CREATE RETURNED == %s%n", info);

            return info;
        }
    }

    @Override
    public FileInfo open(FSP_FILE_SYSTEM fileSystem,
                         String fileName,
                         Set<CreateOptions> createOptions,
                         int grantedAccess) throws NTStatusException {

        verboseOut.printf("== OPEN == %s co=%s ga=%X%n", fileName, createOptions, grantedAccess);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj obj = getObject(filePath);

            FileInfo info = obj.generateFileInfo();
            verboseOut.printf("== OPEN RETURNED == %s%n", info);

            return info;
        }
    }

    @Override
    public FileInfo overwrite(FSP_FILE_SYSTEM fileSystem,
                              String fileName,
                              Set<FileAttributes> fileAttributes,
                              boolean replaceFileAttributes,
                              long allocationSize) throws NTStatusException {

        verboseOut.printf("== OVERWRITE == %s fa=%s replaceFA=%s as=%d%n",
                fileName, fileAttributes, replaceFileAttributes, allocationSize
        );
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);
            if (replaceFileAttributes)
                file.getFileAttributes().clear();
            file.getFileAttributes().addAll(fileAttributes);

            file.setAllocationSize(Math.toIntExact(allocationSize));
            file.setFileSize(0);

            WinSysTime now = WinSysTime.now();
            file.setAccessTime(now);
            file.setWriteTime(now);
            file.setChangeTime(now);

            FileInfo info = file.generateFileInfo();
            verboseOut.printf("== OVERWRITE RETURNED == %s%n", info);

            return info;
        }
    }

    @Override
    public void cleanup(FSP_FILE_SYSTEM fileSystem, String fileName, Set<CleanupFlags> flags) {

        verboseOut.printf("== CLEANUP == %s cf=%s%n", fileName, flags);
        try {
            synchronized (objects) {
                Path filePath = getPath(fileName);
                MemoryObj memObj = getObject(filePath);

                if (flags.contains(CleanupFlags.SET_ARCHIVE_BIT) && memObj instanceof FileObj)
                    memObj.getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_ARCHIVE);

                WinSysTime now = WinSysTime.now();

                if (flags.contains(CleanupFlags.SET_LAST_ACCESS_TIME))
                    memObj.setAccessTime(now);

                if (flags.contains(CleanupFlags.SET_LAST_WRITE_TIME))
                    memObj.setWriteTime(now);

                if (flags.contains(CleanupFlags.SET_CHANGE_TIME))
                    memObj.setChangeTime(now);

                if (flags.contains(CleanupFlags.SET_ALLOCATION_SIZE) && memObj instanceof FileObj)
                    ((FileObj) memObj).adaptAllocationSize(memObj.getFileSize());

                if (flags.contains(CleanupFlags.DELETE)) {
                    if (isNotEmptyDirectory(memObj))
                        return; // abort if trying to remove a non-empty directory
                    removeObject(memObj.getPath());

                    verboseOut.println("== CLEANUP DELETED FILE/DIR ==");
                }
                verboseOut.println("== CLEANUP RETURNED ==");
            }
        } catch (NTStatusException e) {
            // we have no way to pass an error status via cleanup
        }
    }

    @Override
    public void close(FSP_FILE_SYSTEM fileSystem, String fileName) {
        verboseOut.printf("== CLOSE == %s%n", fileName);
    }

    @Override
    public long read(FSP_FILE_SYSTEM fileSystem, String fileName, Pointer pBuffer, long offset, int length)
            throws NTStatusException {

        verboseOut.printf("== READ == %s off=%d len=%d%n", fileName, offset, length);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            int bytesRead = file.read(pBuffer, offset, length);
            verboseOut.printf("== READ RETURNED == bytes=%d%n", bytesRead);

            return bytesRead;
        }
    }

    @Override
    public WriteResult write(FSP_FILE_SYSTEM fileSystem,
                             String fileName,
                             Pointer pBuffer,
                             long offset,
                             int length,
                             boolean writeToEndOfFile,
                             boolean constrainedIo) throws NTStatusException {

        verboseOut.printf("== WRITE == %s off=%d len=%d writeToEnd=%s constrained=%s%n",
                fileName, offset, length, writeToEndOfFile, constrainedIo
        );
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            final long bytesTransferred;
            if (constrainedIo)
                bytesTransferred = file.constrainedWrite(pBuffer, offset, length);
            else
                bytesTransferred = file.write(pBuffer, offset, length, writeToEndOfFile);

            FileInfo info = file.generateFileInfo();
            verboseOut.printf("== WRITE RETURNED == bytes=%d %s%n", bytesTransferred, info);

            return new WriteResult(bytesTransferred, info);
        }
    }

    @Override
    public FileInfo flush(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        verboseOut.printf("== FLUSH == %s%n", fileName);
        synchronized (objects) {
            if (fileName == null)
                return null; // whole volume is being flushed

            Path filePath = getPath(fileName);
            MemoryObj obj = getObject(filePath);

            FileInfo info = obj.generateFileInfo();
            verboseOut.printf("== FLUSH RETURNED == %s%n", info);

            return info;
        }
    }

    @Override
    public FileInfo getFileInfo(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

        verboseOut.printf("== GET FILE INFO == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj obj = getObject(filePath);

            FileInfo info = obj.generateFileInfo();
            verboseOut.printf("== GET FILE INFO RETURNED == %s%n", info);

            return info;
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

        verboseOut.printf("== SET BASIC INFO == %s fa=%s ct=%s ac=%s wr=%s ch=%s%n",
                fileName, fileAttributes, creationTime, lastAccessTime, lastWriteTime, changeTime
        );
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
                obj.setChangeTime(changeTime);

            FileInfo info = obj.generateFileInfo();
            verboseOut.printf("== SET BASIC INFO RETURNED == %s%n", info);

            return info;
        }
    }

    @Override
    public FileInfo setFileSize(FSP_FILE_SYSTEM fileSystem, String fileName, long newSize, boolean setAllocationSize)
            throws NTStatusException {

        verboseOut.printf("== SET FILE SIZE == %s size=%d setAlloc=%s%n", fileName, newSize, setAllocationSize);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            FileObj file = getFileObject(filePath);

            if (setAllocationSize)
                file.setAllocationSize(Math.toIntExact(newSize));
            else
                file.setFileSize(Math.toIntExact(newSize));

            FileInfo info = file.generateFileInfo();
            verboseOut.printf("== SET FILE SIZE RETURNED == %s%n", info);

            return info;
        }
    }

    @Override
    public void canDelete(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

        verboseOut.printf("== CAN DELETE == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            if (isNotEmptyDirectory(memObj))
                throw new NTStatusException(0xC0000101); // STATUS_DIRECTORY_NOT_EMPTY

            verboseOut.println("== CAN DELETE RETURNED ==");
        }
    }

    @Override
    public void rename(FSP_FILE_SYSTEM fileSystem, String oldFileName, String newFileName, boolean replaceIfExists)
            throws NTStatusException {

        verboseOut.printf("== RENAME == %s -> %s%n", oldFileName, newFileName);
        synchronized (objects) {
            Path oldFilePath = getPath(oldFileName);
            Path newFilePath = getPath(newFileName);

            if (hasObject(newFilePath) && !oldFileName.equals(newFileName)) {
                if (!replaceIfExists)
                    throw new NTStatusException(0xC0000035); // STATUS_OBJECT_NAME_COLLISION

                MemoryObj newMemObj = getObject(newFilePath);
                if (newMemObj instanceof DirObj)
                    throw new NTStatusException(0xC0000022); // STATUS_ACCESS_DENIED
            }

            // Rename file or directory (and all existing descendants)
            for (var obj : List.copyOf(objects.values())) {
                if (obj.getPath().startsWith(oldFilePath)) {
                    Path relativePath = oldFilePath.relativize(obj.getPath());
                    Path newObjPath = newFilePath.resolve(relativePath);
                    MemoryObj newObj = removeObject(obj.getPath());
                    newObj.setPath(newObjPath);
                    putObject(newObj);
                }
            }

            verboseOut.println("== RENAME RETURNED ==");
        }
    }

    @Override
    public String getSecurity(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

        verboseOut.printf("== GET SECURITY == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            String securityDescriptor = memObj.getSecurityDescriptor();
            verboseOut.printf("== GET SECURITY RETURNED == %s%n", securityDescriptor);

            return securityDescriptor;
        }
    }

    @Override
    public void setSecurity(FSP_FILE_SYSTEM fileSystem, String fileName, String securityDescriptor)
            throws NTStatusException {

        verboseOut.printf("== SET SECURITY == %s sd=%s%n", fileName, securityDescriptor);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);
            memObj.setSecurityDescriptor(securityDescriptor);

            verboseOut.println("== SET SECURITY RETURNED ==");
        }
    }

    @Override
    public List<FileInfo> readDirectory(FSP_FILE_SYSTEM fileSystem, String fileName, String pattern, String marker)
            throws NTStatusException {

        verboseOut.printf("== READ DIRECTORY == %s pa=%s ma=%s%n", fileName, pattern, marker);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            DirObj dir = getDirObject(filePath);
            List<FileInfo> entries = new ArrayList<>();

            // only add the "." and ".." entries if the directory is not root
            if (!dir.getPath().equals(rootPath)) {
                // handle marker special cases (??)
                if (marker == null)
                    entries.add(dir.generateFileInfo("."));
                if (marker == null || marker.equals(".")) {
                    DirObj parentDir = getParentObject(filePath);
                    entries.add(parentDir.generateFileInfo(".."));
                }
            }

            // include only direct children with relative names
            for (var obj : objects.values()) {
                Path parent = obj.getPath().getParent();
                if (parent != null
                        && parent.equals(dir.getPath())
                        && !obj.getPath().equals(dir.getPath())) {
                    entries.add(obj.generateFileInfo(obj.getName()));
                }
            }

            // sort the entries by file name in a natural order (1, 2, 10, 20, 100, etc.)
            entries.sort(Comparator.comparing(FileInfo::getFileName, NATURAL_ORDER));

            // filter out all results before the marker, if it's set
            if (marker != null) {
                entries = entries.stream()
                        .dropWhile(e -> NATURAL_ORDER.compare(e.getFileName(), marker) <= 0)
                        .collect(Collectors.toList());
            }

            verboseOut.printf("== READ DIRECTORY RETURNED == %s%n", entries);

            return entries;
        }
    }

    @Override
    public FileInfo getDirInfoByName(FSP_FILE_SYSTEM fileSystem, String parentDirName, String fileName)
            throws NTStatusException {

        verboseOut.printf("== GET DIR INFO BY NAME == %s / %s%n", parentDirName, fileName);
        synchronized (objects) {
            Path parentDirPath = getPath(parentDirName);
            getDirObject(parentDirPath); // ensure parent directory exists

            Path filePath = parentDirPath.resolve(fileName).normalize();
            MemoryObj memObj = getObject(filePath);

            FileInfo info = memObj.generateFileInfo(memObj.getName());
            verboseOut.printf("== GET DIR INFO BY NAME RETURNED == %s%n", info);

            return info;
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
        return Objects.toString(filePath, null);
    }

    private boolean hasObject(Path filePath) {
        return objects.containsKey(getPathKey(filePath));
    }

    private MemoryObj getObject(Path filePath) throws NTStatusException {
        MemoryObj obj = objects.get(getPathKey(filePath));
        if (obj == null) {
            getParentObject(filePath); // may throw exception with different status code
            throw new NTStatusException(0xC0000034); // STATUS_OBJECT_NAME_NOT_FOUND
        }

        return obj;
    }

    private DirObj getParentObject(Path filePath) throws NTStatusException {
        MemoryObj parentObj = objects.get(getPathKey(filePath.getParent()));
        if (parentObj == null)
            throw new NTStatusException(0xC000003A); // STATUS_OBJECT_PATH_NOT_FOUND
        if (!(parentObj instanceof DirObj))
            throw new NTStatusException(0xC0000103); // STATUS_NOT_A_DIRECTORY

        return (DirObj)parentObj;
    }

    private void putObject(MemoryObj obj) {
        objects.put(getPathKey(obj.getPath()), obj);
        obj.touchParent();
    }

    private MemoryObj removeObject(Path filePath) {
        MemoryObj obj = objects.remove(getPathKey(filePath));
        if (obj != null)
            obj.touchParent();
        return obj;
    }

    private FileObj getFileObject(Path filePath) throws NTStatusException {
        MemoryObj obj = getObject(filePath);
        if (!(obj instanceof FileObj))
            throw new NTStatusException(0xC00000BA); // STATUS_FILE_IS_A_DIRECTORY

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
