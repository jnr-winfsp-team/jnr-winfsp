package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.lib.LibKernel32;
import com.github.jnrwinfspteam.jnrwinfsp.lib.LibWinFsp;
import com.github.jnrwinfspteam.jnrwinfsp.lib.WinPathUtils;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultSecurityAndAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultVolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.struct.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_VOLUME_PARAMS.FSAttr;
import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
                        stringBytes(FSP.FSCTL_DISK_DEVICE_NAME),
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
                        pathBytes(mountPoint)
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
        vp.setFileSystemAttribute(FSAttr.FlushAndPurgeOnCleanup, true);
        vp.setFileSystemAttribute(FSAttr.UmFileContextIsUserContext2, true);
        vp.setFileSystemAttribute(FSAttr.UmFileContextIsFullContext, false);
    }

    private void initFSInterface(Runtime runtime) {
        fsInterfaceP = FSP_FILE_SYSTEM_INTERFACE.create(runtime);
        FSP_FILE_SYSTEM_INTERFACE fsi = fsInterfaceP.get();

        var winfsp = this;

        if (isImplemented("getVolumeInfo")) {
            fsi.GetVolumeInfo.set((pFS, pVolumeInfo) -> {
                ResultVolumeInfo res = winfsp.getVolumeInfo(fs(pFS));
                if (res.getNtStatus() == 0) {
                    FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                    viOut.TotalSize.set(res.getTotalSize());
                    viOut.FreeSize.set(res.getFreeSize());
                    viOut.setVolumeLabel(res.getVolumeLabel());
                }

                return res.getNtStatus();
            });
        }
        if (isImplemented("setVolumeLabel")) {
            fsi.SetVolumeLabel.set((pFS, pVolumeLabel, pVolumeInfo) -> {
                ResultVolumeInfo res = winfsp.setVolumeLabel(fs(pFS), string(pVolumeLabel, 32));
                if (res.getNtStatus() == 0) {
                    FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                    viOut.TotalSize.set(res.getTotalSize());
                    viOut.FreeSize.set(res.getFreeSize());
                    viOut.setVolumeLabel(res.getVolumeLabel());
                }

                return res.getNtStatus();
            });
        }
        if (isImplemented("getSecurityByName")) {
            fsi.GetSecurityByName.set((pFS,
                                       pFileName,
                                       pFileAttributes,
                                       pSecurityDescriptor,
                                       pSecurityDescriptorSize) -> {
                ResultSecurityAndAttributes res = winfsp.getSecurityByName(fs(pFS), string(pFileName, 260));
                if (res.getNtStatus() == 0 || res.getNtStatus() == 0x104) {
                    if (pFileAttributes != null)
                        pFileAttributes.putInt(0, res.getFileAttributes());

                    // Get file security
                    if (pSecurityDescriptorSize != null) {
                        int sdSize = res.getSecurityDescriptorSize();
                        if (sdSize > pSecurityDescriptorSize.getInt(0)) {
                            // In case of overflow error, WinFsp will retry with a new
                            // allocation based on `pSecurityDescriptorSize`. Hence we
                            // must update this value to the required size.
                            pSecurityDescriptorSize.putInt(0, sdSize);
                            return 0x80000005; // STATUS_BUFFER_OVERFLOW
                        }

                        pSecurityDescriptorSize.putInt(0, sdSize);
                        if (pSecurityDescriptor != null) {
                            pSecurityDescriptor.transferFrom(0, res.getSecurityDescriptor(), 0, sdSize);
                        }
                    }
                }

                return res.getNtStatus();
            });
        }
        if (isImplemented("create")) {
//            fsi.Create.set((pFS,
//                            pFileName,
//                            createOptions,
//                            grantedAccess,
//                            fileAttributes,
//                            pSecurityDescriptor,
//                            allocationSize,
//                            ppFileContext,
//                            pFileInfo) -> {
//
//            });
        }
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

    private static FSP_FILE_SYSTEM fs(Pointer pFS) {
        return FSP_FILE_SYSTEM.of(pFS).get();
    }

    private static String string(Pointer pStr, int maxLength) {
        return pStr.getString(0, maxLength, StandardCharsets.UTF_16LE);
    }

    private static byte[] stringBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_16LE);
    }

    private static byte[] pathBytes(Path path) {
        if (path == null)
            return null;
        else
            return stringBytes(path.toString());
    }
}
