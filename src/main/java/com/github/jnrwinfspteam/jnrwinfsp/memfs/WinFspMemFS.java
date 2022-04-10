package com.github.jnrwinfspteam.jnrwinfsp.memfs;

import com.github.jnrwinfspteam.jnrwinfsp.api.*;
import com.github.jnrwinfspteam.jnrwinfsp.WinFspStubFS;
import com.github.jnrwinfspteam.jnrwinfsp.internal.struct.FSP_FILE_SYSTEM;
import com.github.jnrwinfspteam.jnrwinfsp.service.ServiceException;
import com.github.jnrwinfspteam.jnrwinfsp.service.ServiceRunner;
import com.github.jnrwinfspteam.jnrwinfsp.util.NaturalOrderComparator;
import jnr.ffi.Pointer;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * A simple in-memory file system.
 */
public class WinFspMemFS extends WinFspStubFS {
    public static void main(String[] args) throws NTStatusException, ServiceException {
        Path mountPoint = null;
        if (args.length > 0)
            mountPoint = Path.of(args[0]);

        var memFS = new WinFspMemFS();
        System.out.printf("Mounting %s ...%n", mountPoint == null ? "" : mountPoint);
        ServiceRunner.mountLocalDriveAsService("WinFspMemFS", memFS, mountPoint, new MountOptions()
                .setDebug(false)
                .setCase(MountOptions.CaseOption.CASE_SENSITIVE)
                .setSectorSize(512)
                .setSectorsPerAllocationUnit(1)
                .setForceBuiltinAdminOwnerAndGroup(true)
        );
    }


    private static final String ROOT_SECURITY_DESCRIPTOR = "O:BAG:BAD:PAR(A;OICI;FA;;;SY)(A;OICI;FA;;;BA)(A;OICI;FA;;;WD)";
    private static final Comparator<String> NATURAL_ORDER = new NaturalOrderComparator();
    private static final long MAX_FILE_NODES = 10240;
    private static final long MAX_FILE_SIZE = 16 * 1024 * 1024;

    private final Path rootPath;
    private final Map<String, MemoryObj> objects;

    private long nextIndexNumber;
    private String volumeLabel;

    private final PrintStream verboseOut;

    public WinFspMemFS() throws NTStatusException {
        this(false);
    }

    public WinFspMemFS(boolean verbose) throws NTStatusException {
        this.rootPath = Path.of("\\").normalize();
        this.objects = new HashMap<>();
        this.objects.put(rootPath.toString(), new DirObj(
                null,
                rootPath,
                securityDescriptorToBytes(ROOT_SECURITY_DESCRIPTOR),
                null
        ));

        this.nextIndexNumber = 1L;
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
    public Optional<SecurityResult> getSecurityByName(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        verboseOut.printf("== GET SECURITY BY NAME == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            if (!hasObject(filePath))
                return Optional.empty();

            MemoryObj obj = getObject(filePath);
            byte[] securityDescriptor = obj.getSecurityDescriptor();
            FileInfo info = obj.generateFileInfo();
            verboseOut.printf("== GET SECURITY BY NAME RETURNED == %s %s%n",
                    securityDescriptorToString(securityDescriptor), info);

            return Optional.of(new SecurityResult(securityDescriptor, EnumSet.copyOf(obj.getFileAttributes())));
        }
    }

    @Override
    public FileInfo create(FSP_FILE_SYSTEM fileSystem,
                           String fileName,
                           Set<CreateOptions> createOptions,
                           int grantedAccess,
                           Set<FileAttributes> fileAttributes,
                           byte[] securityDescriptor,
                           long allocationSize,
                           ReparsePoint reparsePoint) throws NTStatusException {

        verboseOut.printf("== CREATE == %s co=%s ga=%X fa=%s sd=%s as=%d rp=%s%n",
                fileName, createOptions, grantedAccess, fileAttributes,
                securityDescriptorToString(securityDescriptor), allocationSize, reparsePoint
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
                obj = new DirObj(parent, filePath, securityDescriptor, reparsePoint);
            else {
                var file = new FileObj(parent, filePath, securityDescriptor, reparsePoint);
                file.setAllocationSize(Math.toIntExact(allocationSize));
                obj = file;
            }

            obj.getFileAttributes().addAll(fileAttributes);
            obj.setIndexNumber(nextIndexNumber++);
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
    public byte[] getSecurity(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {

        verboseOut.printf("== GET SECURITY == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            byte[] securityDescriptor = memObj.getSecurityDescriptor();
            verboseOut.printf("== GET SECURITY RETURNED == %s%n", securityDescriptorToString(securityDescriptor));

            return securityDescriptor;
        }
    }

    @Override
    public void setSecurity(FSP_FILE_SYSTEM fileSystem, String fileName, byte[] securityDescriptor)
            throws NTStatusException {

        verboseOut.printf("== SET SECURITY == %s sd=%s%n", fileName, securityDescriptorToString(securityDescriptor));
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);
            memObj.setSecurityDescriptor(securityDescriptor);

            verboseOut.println("== SET SECURITY RETURNED ==");
        }
    }

    @Override
    public void readDirectory(FSP_FILE_SYSTEM fileSystem,
                              String fileName,
                              String pattern,
                              String marker,
                              Predicate<FileInfo> consumer) throws NTStatusException {

        verboseOut.printf("== READ DIRECTORY == %s pa=%s ma=%s%n", fileName, pattern, marker);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            DirObj dir = getDirObject(filePath);

            // only add the "." and ".." entries if the directory is not root
            if (!dir.getPath().equals(rootPath)) {
                if (marker == null)
                    if (!consumer.test(dir.generateFileInfo(".")))
                        return;
                if (marker == null || marker.equals(".")) {
                    DirObj parentDir = getParentObject(filePath);
                    if (!consumer.test(parentDir.generateFileInfo("..")))
                        return;
                    marker = null;
                }
            }

            final String finalMarker = marker;
            objects.values().stream()
                    .filter(obj -> obj.getParent() != null &&
                            obj.getParent().getPath().equals(dir.getPath()))
                    .sorted(Comparator.comparing(MemoryObj::getName, NATURAL_ORDER))
                    .dropWhile(obj -> isBeforeMarker(obj.getName(), finalMarker))
                    .map(obj -> obj.generateFileInfo(obj.getName()))
                    .takeWhile(consumer)
                    .forEach(o -> {
                    });
        }
    }

    private static boolean isBeforeMarker(String name, String marker) {
        return marker != null && NATURAL_ORDER.compare(name, marker) <= 0;
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

    @Override
    public byte[] getReparsePointData(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        verboseOut.printf("== GET REPARSE POINT DATA == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            if (!memObj.getFileAttributes().contains(FileAttributes.FILE_ATTRIBUTE_REPARSE_POINT))
                throw new NTStatusException(0xC0000275); // STATUS_NOT_A_REPARSE_POINT

            byte[] reparseData = memObj.getReparseData();
            verboseOut.printf("== GET REPARSE POINT DATA RETURNED == %s%n", Arrays.toString(reparseData));

            return reparseData;
        }
    }

    @Override
    public void setReparsePoint(FSP_FILE_SYSTEM fileSystem, String fileName, byte[] reparseData, int reparseTag) throws NTStatusException {
        verboseOut.printf("== SET REPARSE POINT == %s rd=%s rt=%d%n",
                fileName, Arrays.toString(reparseData), reparseTag
        );
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            if (isNotEmptyDirectory(memObj))
                throw new NTStatusException(0xC0000101); // STATUS_DIRECTORY_NOT_EMPTY

            memObj.setReparseData(reparseData);
            memObj.setReparseTag(reparseTag);
            memObj.getFileAttributes().add(FileAttributes.FILE_ATTRIBUTE_REPARSE_POINT);
        }
    }

    @Override
    public void deleteReparsePoint(FSP_FILE_SYSTEM fileSystem, String fileName) throws NTStatusException {
        verboseOut.printf("== DELETE REPARSE POINT == %s%n", fileName);
        synchronized (objects) {
            Path filePath = getPath(fileName);
            MemoryObj memObj = getObject(filePath);

            if (!memObj.getFileAttributes().contains(FileAttributes.FILE_ATTRIBUTE_REPARSE_POINT))
                throw new NTStatusException(0xC0000275); // STATUS_NOT_A_REPARSE_POINT

            memObj.setReparseData(null);
            memObj.setReparseTag(0);
            memObj.getFileAttributes().remove(FileAttributes.FILE_ATTRIBUTE_REPARSE_POINT);
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

        return (DirObj) parentObj;
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
