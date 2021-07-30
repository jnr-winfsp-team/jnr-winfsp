package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.lib.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_VOLUME_PARAMS.FSAttr;
import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * If you extend this class directly, then you will need to implement all the operations.
 * <p>
 * See {@link WinFspStubFS} for a way to implement only a subset of the operations.
 */
public abstract class AbstractWinFspFS implements WinFspFS {

    private final LibWinFsp libWinFsp;
    private final LibKernel32 libKernel32;
    private final LibAdvapi32 libAdvapi32;
    private final Object mountLock;
    private boolean mounted;
    private final Set<String> notImplementedMethods;

    private Pointered<FSP_FSCTL_VOLUME_PARAMS> volumeParamsP;
    private Pointered<FSP_FILE_SYSTEM_INTERFACE> fsInterfaceP;
    private Pointer pFileSystem;

    public AbstractWinFspFS() {
        this.libWinFsp = LibraryLoader.create(LibWinFsp.class)
                .library(WinPathUtils.getWinFspPath())
                .failImmediately()
                .load();
        this.libKernel32 = LibraryLoader.create(LibKernel32.class)
                .library("kernel32.dll")
                .failImmediately()
                .load();
        this.libAdvapi32 = LibraryLoader.create(LibAdvapi32.class)
                .library("Advapi32.dll")
                .failImmediately()
                .load();
        this.mountLock = new Object();
        this.mounted = false;
        this.notImplementedMethods = Arrays.stream(this.getClass().getMethods())
                .filter(method -> method.getAnnotation(NotImplemented.class) != null)
                .map(Method::getName)
                .collect(Collectors.toUnmodifiableSet());

        this.volumeParamsP = null;
        this.fsInterfaceP = null;
        this.pFileSystem = null;
    }

    @Override
    public void mountLocalDrive(Path mountPoint, boolean debug) throws MountException {
        synchronized (mountLock) {
            if (mounted)
                throw new MountException("WinFsp local drive is already mounted");

            try {
                Runtime runtime = Runtime.getSystemRuntime();
                initVolumeParams(runtime);
                initFSInterface(runtime);

                var ppFileSystem = new PointerByReference();
                checkMountStatus("FileSystemCreate", libWinFsp.FspFileSystemCreate(
                        FSP.FSCTL_DISK_DEVICE_NAME.toCharArray(),
                        volumeParamsP.getPointer(),
                        fsInterfaceP.getPointer(),
                        ppFileSystem
                ));
                pFileSystem = ppFileSystem.getValue();

                if (debug) {
                    Pointer stdErrHandle = libKernel32.GetStdHandle(LibKernel32.STD_ERROR_HANDLE);
                    libWinFsp.FspDebugLogSetHandle(stdErrHandle);
                    libWinFsp.FspFileSystemSetDebugLogF(pFileSystem, -1);
                }

                checkMountStatus("SetMountPoint", libWinFsp.FspFileSystemSetMountPoint(
                        pFileSystem,
                        mountPoint == null ? null : mountPoint.toString().toCharArray()
                ));

                checkMountStatus("StartDispatcher", libWinFsp.FspFileSystemStartDispatcher(
                        pFileSystem,
                        0
                ));

            } catch (Throwable t) {
                if (pFileSystem != null) {
                    libWinFsp.FspFileSystemStopDispatcher(pFileSystem);
                    libWinFsp.FspFileSystemRemoveMountPoint(pFileSystem);
                    libWinFsp.FspFileSystemDelete(pFileSystem);
                }
                freeStructs();
                throw t;
            }

            mounted = true;
        }
    }

    @Override
    public void unmountLocalDrive() {
        synchronized (mountLock) {
            if (!mounted)
                return;

            if (pFileSystem != null) {
                libWinFsp.FspFileSystemStopDispatcher(pFileSystem);
                libWinFsp.FspFileSystemRemoveMountPoint(pFileSystem);
                libWinFsp.FspFileSystemDelete(pFileSystem);
            }
            freeStructs();

            mounted = false;
        }
    }

    private void initVolumeParams(Runtime runtime) {
        volumeParamsP = FSP_FSCTL_VOLUME_PARAMS.create(runtime);
        FSP_FSCTL_VOLUME_PARAMS vp = volumeParamsP.get();

        // TODO set these as configurable
        vp.SectorSize.set(4096);
        vp.SectorsPerAllocationUnit.set(1);
        vp.VolumeCreationTime.set(WinSysTime.now().get());
        vp.VolumeSerialNumber.set(0);
        vp.FileInfoTimeout.set(1000);
        vp.setFileSystemAttribute(FSAttr.CaseSensitiveSearch, false);
        vp.setFileSystemAttribute(FSAttr.CasePreservedNames, true);
        vp.setFileSystemAttribute(FSAttr.UnicodeOnDisk, true);
        vp.setFileSystemAttribute(FSAttr.PersistentAcls, true);
        vp.setFileSystemAttribute(FSAttr.PostCleanupWhenModifiedOnly, true);
        vp.setFileSystemAttribute(FSAttr.PassQueryDirectoryPattern, true);
        vp.setFileSystemAttribute(FSAttr.PassQueryDirectoryFileName, true);
        vp.setFileSystemAttribute(FSAttr.FlushAndPurgeOnCleanup, true);
        vp.setFileSystemAttribute(FSAttr.UmFileContextIsUserContext2, true);
        vp.setFileSystemAttribute(FSAttr.UmFileContextIsFullContext, false);
    }

    private void initFSInterface(Runtime runtime) {
        fsInterfaceP = FSP_FILE_SYSTEM_INTERFACE.create(runtime);
        FSP_FILE_SYSTEM_INTERFACE fsi = fsInterfaceP.get();

        if (isImplemented("getVolumeInfo"))
            FSHelper.initGetVolumeInfo(fsi, this);
        if (isImplemented("setVolumeLabel"))
            FSHelper.initSetVolumeLabel(fsi, this);
        if (isImplemented("getSecurityByName"))
            FSHelper.initGetSecurityByName(fsi, this, this.libWinFsp, this.libKernel32, this.libAdvapi32);
        if (isImplemented("create"))
            FSHelper.initCreate(fsi, this, this.libWinFsp, this.libKernel32, this.libAdvapi32);
        if (isImplemented("open"))
            FSHelper.initOpen(fsi, this);
        if (isImplemented("overwrite"))
            FSHelper.initOverwrite(fsi, this);
        if (isImplemented("cleanup"))
            FSHelper.initCleanup(fsi, this);
        if (isImplemented("close"))
            FSHelper.initClose(fsi, this);
        if (isImplemented("read"))
            FSHelper.initRead(fsi, this);
        if (isImplemented("write"))
            FSHelper.initWrite(fsi, this);
        if (isImplemented("flush"))
            FSHelper.initFlush(fsi, this);
        if (isImplemented("getFileInfo"))
            FSHelper.initGetFileInfo(fsi, this);
        if (isImplemented("setBasicInfo"))
            FSHelper.initSetBasicInfo(fsi, this);
        if (isImplemented("setFileSize"))
            FSHelper.initSetFileSize(fsi, this);
        if (isImplemented("canDelete"))
            FSHelper.initCanDelete(fsi, this);
        if (isImplemented("rename"))
            FSHelper.initRename(fsi, this);
        if (isImplemented("getSecurity"))
            FSHelper.initGetSecurity(fsi, this, this.libWinFsp, this.libKernel32, this.libAdvapi32);
        if (isImplemented("setSecurity") && isImplemented("getSecurity"))
            FSHelper.initSetSecurity(fsi, this, this.libWinFsp, this.libKernel32, this.libAdvapi32);
        if (isImplemented("readDirectory"))
            FSHelper.initReadDirectory(fsi, this, this.libWinFsp);
        if (isImplemented("getDirInfoByName"))
            FSHelper.initGetDirInfoByName(fsi, this);
    }

    private boolean isImplemented(String funcName) {
        return !notImplementedMethods.contains(funcName);
    }

    private void freeStructs() {
        if (volumeParamsP != null) {
            volumeParamsP.free();
            volumeParamsP = null;
        }
        if (fsInterfaceP != null) {
            fsInterfaceP.free();
            fsInterfaceP = null;
        }
        if (pFileSystem != null) {
            pFileSystem = null;
        }
    }

    private static void checkMountStatus(String function, int ntStatus) throws MountException {
        if (ntStatus != 0) {
            throw new MountException(function + " error", ntStatus);
        }
    }
}
