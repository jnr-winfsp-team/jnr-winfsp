package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.api.*;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.FSP;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibKernel32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import com.github.jnrwinfspteam.jnrwinfsp.internal.struct.FSP_FILE_SYSTEM_INTERFACE;
import com.github.jnrwinfspteam.jnrwinfsp.internal.struct.FSP_FSCTL_VOLUME_PARAMS;
import com.github.jnrwinfspteam.jnrwinfsp.internal.struct.FSP_FSCTL_VOLUME_PARAMS.FSAttr;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.Pointered;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.StringUtils;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.byref.PointerByReference;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * If you extend this class directly, then you will need to implement all the operations.
 * <p>
 * See {@link WinFspStubFS} for a way to implement only a subset of the operations.
 */
public abstract class AbstractWinFspFS implements Mountable, WinFspFS {

    private final Object mountLock;
    private boolean mounted;
    private final Set<String> notImplementedMethods;

    private FSHelper fsHelper;
    private Pointered<FSP_FSCTL_VOLUME_PARAMS> volumeParamsP;
    private Pointered<FSP_FILE_SYSTEM_INTERFACE> fsInterfaceP;
    private Pointer pFileSystem;

    public AbstractWinFspFS() {
        this.mountLock = new Object();
        this.mounted = false;
        this.notImplementedMethods = Arrays.stream(this.getClass().getMethods())
                .filter(method -> method.getAnnotation(NotImplemented.class) != null)
                .map(Method::getName)
                .collect(Collectors.toUnmodifiableSet());

        this.fsHelper = null;
        this.volumeParamsP = null;
        this.fsInterfaceP = null;
        this.pFileSystem = null;
    }

    @Override
    public final void mountLocalDrive(Path mountPoint, MountOptions options) throws MountException {
        Objects.requireNonNull(options);

        synchronized (mountLock) {
            if (mounted)
                throw new MountException("WinFsp local drive is already mounted", 0xC01C001A); // STATUS_FLT_VOLUME_ALREADY_MOUNTED

            try {
                Runtime runtime = Runtime.getSystemRuntime();
                initVolumeParams(runtime, options);
                initFSInterface(runtime, options);

                var ppFileSystem = new PointerByReference();
                checkMountStatus("FileSystemCreate", LibWinFsp.INSTANCE.FspFileSystemCreate(
                        StringUtils.toBytes(FSP.FSCTL_DISK_DEVICE_NAME, true),
                        volumeParamsP.getPointer(),
                        fsInterfaceP.getPointer(),
                        ppFileSystem
                ));
                pFileSystem = ppFileSystem.getValue();

                if (options.hasDebug()) {
                    Pointer stdErrHandle = LibKernel32.INSTANCE.GetStdHandle(LibKernel32.STD_ERROR_HANDLE);
                    LibWinFsp.INSTANCE.FspDebugLogSetHandle(stdErrHandle);
                    LibWinFsp.INSTANCE.FspFileSystemSetDebugLogF(pFileSystem, -1);
                }

                checkMountStatus("SetMountPoint", LibWinFsp.INSTANCE.FspFileSystemSetMountPoint(
                        pFileSystem,
                        mountPoint == null ? null : StringUtils.toBytes(mountPoint.toString(), true)
                ));

                checkMountStatus("StartDispatcher", LibWinFsp.INSTANCE.FspFileSystemStartDispatcher(
                        pFileSystem,
                        0
                ));

            } catch (Throwable t) {
                if (pFileSystem != null) {
                    LibWinFsp.INSTANCE.FspFileSystemStopDispatcher(pFileSystem);
                    LibWinFsp.INSTANCE.FspFileSystemRemoveMountPoint(pFileSystem);
                    LibWinFsp.INSTANCE.FspFileSystemDelete(pFileSystem);
                }
                freeStructs();
                throw t;
            }

            mounted = true;
        }
    }

    @Override
    public final void unmountLocalDrive() {
        synchronized (mountLock) {
            if (!mounted)
                return;

            if (pFileSystem != null) {
                LibWinFsp.INSTANCE.FspFileSystemStopDispatcher(pFileSystem);
                LibWinFsp.INSTANCE.FspFileSystemRemoveMountPoint(pFileSystem);
                LibWinFsp.INSTANCE.FspFileSystemDelete(pFileSystem);
            }
            freeStructs();

            mounted = false;
        }
    }

    private void initVolumeParams(Runtime runtime, MountOptions options) {
        volumeParamsP = FSP_FSCTL_VOLUME_PARAMS.create(runtime);
        FSP_FSCTL_VOLUME_PARAMS vp = volumeParamsP.get();

        vp.SectorSize.set(options.getSectorSize());
        vp.SectorsPerAllocationUnit.set(options.getSectorsPerAllocationUnit());
        vp.VolumeCreationTime.set(WinSysTime.now().get());
        vp.VolumeSerialNumber.set(WinSysTime.now().get() / (10000 * 1000));
        vp.FileInfoTimeout.set(options.getFileInfoTimeout());
        vp.MaxComponentLength.set(options.getMaxFileNameLength());
        vp.setFileSystemAttribute(FSAttr.UnicodeOnDisk, true);
        vp.setFileSystemAttribute(FSAttr.PersistentAcls, true);
        vp.setFileSystemAttribute(FSAttr.ReparsePoints, true);
        vp.setFileSystemAttribute(FSAttr.PostCleanupWhenModifiedOnly, true);
        vp.setFileSystemAttribute(FSAttr.PassQueryDirectoryPattern, true);
        vp.setFileSystemAttribute(FSAttr.PassQueryDirectoryFileName, true);
        vp.setFileSystemAttribute(FSAttr.FlushAndPurgeOnCleanup, true);
        vp.setFileSystemAttribute(FSAttr.UmFileContextIsUserContext2, true);
        vp.setFileSystemAttribute(FSAttr.UmFileContextIsFullContext, false);
        vp.setFileSystemAttribute(FSAttr.AllowOpenInKernelMode, true);
        vp.setFileSystemAttribute(FSAttr.RejectIrpPriorToTransact0, true);
        vp.setFileSystemAttribute(FSAttr.WslFeatures, options.hasWslFeatures());

        switch (options.getCaseOption()) {
            case CASE_SENSITIVE:
                vp.setFileSystemAttribute(FSAttr.CaseSensitiveSearch, true);
                vp.setFileSystemAttribute(FSAttr.CasePreservedNames, true);
                break;
            case CASE_PRESERVING:
                vp.setFileSystemAttribute(FSAttr.CaseSensitiveSearch, false);
                vp.setFileSystemAttribute(FSAttr.CasePreservedNames, true);
                break;
            case CASE_INSENSITIVE:
                vp.setFileSystemAttribute(FSAttr.CaseSensitiveSearch, false);
                vp.setFileSystemAttribute(FSAttr.CasePreservedNames, false);
                break;
        }
    }

    private void initFSInterface(Runtime runtime, MountOptions options) throws MountException {
        fsHelper = new FSHelper(this, options);
        fsInterfaceP = FSP_FILE_SYSTEM_INTERFACE.create(runtime);
        FSP_FILE_SYSTEM_INTERFACE fsi = fsInterfaceP.get();

        if (isImplemented("getVolumeInfo"))
            fsHelper.initGetVolumeInfo(fsi);
        if (isImplemented("setVolumeLabel"))
            fsHelper.initSetVolumeLabel(fsi);
        if (isImplemented("getSecurityByName"))
            fsHelper.initGetSecurityByName(fsi);
        if (isImplemented("create"))
            fsHelper.initCreateEx(fsi);
        if (isImplemented("open"))
            fsHelper.initOpen(fsi);
        if (isImplemented("overwrite"))
            fsHelper.initOverwrite(fsi);
        if (isImplemented("cleanup"))
            fsHelper.initCleanup(fsi);
        if (isImplemented("close"))
            fsHelper.initClose(fsi);
        if (isImplemented("read"))
            fsHelper.initRead(fsi);
        if (isImplemented("write"))
            fsHelper.initWrite(fsi);
        if (isImplemented("flush"))
            fsHelper.initFlush(fsi);
        if (isImplemented("getFileInfo"))
            fsHelper.initGetFileInfo(fsi);
        if (isImplemented("setBasicInfo"))
            fsHelper.initSetBasicInfo(fsi);
        if (isImplemented("setFileSize"))
            fsHelper.initSetFileSize(fsi);
        if (isImplemented("canDelete"))
            fsHelper.initCanDelete(fsi);
        if (isImplemented("rename"))
            fsHelper.initRename(fsi);
        if (isImplemented("getSecurity"))
            fsHelper.initGetSecurity(fsi);
        if (isImplemented("setSecurity") && isImplemented("getSecurity"))
            fsHelper.initSetSecurity(fsi);
        if (isImplemented("readDirectory"))
            fsHelper.initReadDirectory(fsi);
        if (isImplemented("getDirInfoByName"))
            fsHelper.initGetDirInfoByName(fsi);
        if (isImplemented("getReparsePointData"))
            fsHelper.initResolveReparsePoints(fsi);
        if (isImplemented("getReparsePointData"))
            fsHelper.initGetReparsePoint(fsi);
        if (isImplemented("getReparsePointData") && isImplemented("setReparsePoint"))
            fsHelper.initSetReparsePoint(fsi);
        if (isImplemented("getReparsePointData") && isImplemented("deleteReparsePoint"))
            fsHelper.initDeleteReparsePoint(fsi);
    }

    private boolean isImplemented(String funcName) {
        return !notImplementedMethods.contains(funcName);
    }

    private void freeStructs() {
        if (fsHelper != null) {
            fsHelper.free();
            fsHelper = null;
        }
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
