package com.github.jnrwinfspteam.jnrwinfsp.api;

import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibAdvapi32;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import com.github.jnrwinfspteam.jnrwinfsp.internal.struct.*;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.PointerUtils;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.Pointered;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.SecurityDescriptorUtils;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.StringUtils;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.types.size_t;

import java.io.PrintStream;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class FSHelper {

    private static final Runtime RUNTIME = Runtime.getSystemRuntime();
    private static final DateTimeFormatter ERROR_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final WinFspFS winfsp;
    private final PrintStream verboseErr;
    private final LibWinFsp.GetReparsePointByNameCallback getReparsePointCallback;
    private Pointer builtInAdminSID;

    private final ConcurrentMap<Long, OpenContext> openContexts;

    FSHelper(WinFspFS winfsp, MountOptions options) throws MountException {
        this.winfsp = Objects.requireNonNull(winfsp);
        this.verboseErr = options.getErrorPrinter() != null ? options.getErrorPrinter()
                : (options.hasDebug() ? System.err : null);
        this.getReparsePointCallback = newGetReparsePointByNameCallback();

        try {
            if (options.hasForceBuiltinAdminOwnerAndGroup()) {
                this.builtInAdminSID = SecurityDescriptorUtils.getLocalWellKnownSID(
                        RUNTIME,
                        LibAdvapi32.WinBuiltinAdministratorsSid
                );
            }
            else {
                this.builtInAdminSID = null;
            }
        } catch (NTStatusException e) {
            throw new MountException("Could not retrieve well-known SID for 'Built-in Administrators'", e);
        }

        this.openContexts = new ConcurrentHashMap<>();
    }

    void free() {
        if (this.builtInAdminSID != null) {
            PointerUtils.freeBytesPointer(this.builtInAdminSID);
            this.builtInAdminSID = null;
        }

        openContexts.clear();
    }

    void initGetVolumeInfo(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetVolumeInfo.set((pFS, pVolumeInfo) -> {

            try {
                VolumeInfo vi = winfsp.getVolumeInfo();
                FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                viOut.TotalSize.set(vi.getTotalSize());
                viOut.FreeSize.set(vi.getFreeSize());
                viOut.setVolumeLabel(vi.getVolumeLabel());

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "GetVolumeInfo");
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "GetVolumeInfo");
                throw e;
            }
        });
    }

    void initSetVolumeLabel(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetVolumeLabel.set((pFS, pVolumeLabel, pVolumeInfo) -> {

            try {
                VolumeInfo vi = winfsp.setVolumeLabel(StringUtils.fromPointer(pVolumeLabel));

                FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                viOut.TotalSize.set(vi.getTotalSize());
                viOut.FreeSize.set(vi.getFreeSize());
                viOut.setVolumeLabel(vi.getVolumeLabel());

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "SetVolumeLabel");
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "SetVolumeLabel");
                throw e;
            }
        });
    }

    void initGetSecurityByName(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetSecurityByName.set((pFS, pFileName, pFileAttributes, pSecurityDescriptor, pSecurityDescriptorSize) -> {

            String fileName = null;
            try {
                fileName = StringUtils.fromPointer(pFileName);

                Optional<SecurityResult> opSR = winfsp.getSecurityByName(fileName);
                if (opSR.isEmpty()) {
                    byte res = LibWinFsp.INSTANCE.FspFileSystemFindReparsePoint(
                            pFS,
                            this.getReparsePointCallback,
                            null,
                            pFileName,
                            pFileAttributes // this stores the reparse point index in case res is TRUE
                    );

                    if (bool(res))
                        throw new NTStatusException(0x00000104); // STATUS_REPARSE
                    else
                        throw new NTStatusException(0xC0000034); // STATUS_OBJECT_NAME_NOT_FOUND
                }

                SecurityResult sr = opSR.orElseThrow();
                if (pFileAttributes != null)
                    pFileAttributes.putInt(0, FileAttributes.intOf(sr.getFileAttributes()));

                SecurityDescriptorUtils.fromBytes(
                        RUNTIME,
                        sr.getSecurityDescriptor(),
                        pSecurityDescriptor,
                        pSecurityDescriptorSize
                );

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "GetSecurityByName", fileName);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "GetSecurityByName", fileName);
                throw e;
            }
        });
    }

    void initCreateEx(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.CreateEx.set((pFS, pFileName, createOptions, grantedAccess, fileAttributes, pSecurityDescriptor, allocationSize,
                          pExtraBuffer, extraLength, extraBufferIsReparsePoint,
                          ppFileContext, pFileInfo) -> {

            String fileName = null;
            try {
                fileName = StringUtils.fromPointer(pFileName);

                final byte[] securityDescriptor;
                if (this.builtInAdminSID == null) {
                    securityDescriptor = SecurityDescriptorUtils.toBytes(pSecurityDescriptor);
                }
                else {
                    Pointer pSDWithOwnerAndGroupSet = SecurityDescriptorUtils.setOwnerAndGroup(
                            RUNTIME,
                            pSecurityDescriptor,
                            builtInAdminSID,
                            builtInAdminSID
                    );
                    securityDescriptor = SecurityDescriptorUtils.toBytes(pSDWithOwnerAndGroupSet);
                    PointerUtils.freeMemory(pSDWithOwnerAndGroupSet); // avoid memory leak
                }

                ReparsePoint reparsePoint = null;
                if (pExtraBuffer != null && bool(extraBufferIsReparsePoint)) {
                    byte[] reparsePointData = PointerUtils.getBytes(pExtraBuffer, 0, (int) extraLength);
                    int reparseTag = pExtraBuffer.getInt(0); /* the first field in a reparse buffer is the tag */
                    reparsePoint = new ReparsePoint(reparsePointData, reparseTag);
                }

                OpenResult res = winfsp.create(
                        fileName,
                        CreateOptions.setOf(createOptions),
                        grantedAccess,
                        FileAttributes.setOf(fileAttributes),
                        securityDescriptor,
                        allocationSize,
                        reparsePoint
                );

                putFileContext(ppFileContext, res);
                putOpenFileInfo(pFileInfo, res.getFileInfo());

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "CreateEx", fileName);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "CreateEx", fileName);
                throw e;
            }
        });
    }

    void initOpen(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Open.set((pFS, pFileName, createOptions, grantedAccess, ppFileContext, pFileInfo) -> {

            String fileName = null;
            try {
                fileName = StringUtils.fromPointer(pFileName);

                OpenResult res = winfsp.open(
                        fileName,
                        CreateOptions.setOf(createOptions),
                        grantedAccess
                );

                putFileContext(ppFileContext, res);
                putOpenFileInfo(pFileInfo, res.getFileInfo());

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "Open", fileName);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "Open", fileName);
                throw e;
            }
        });
    }

    void initOverwrite(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Overwrite.set((pFS, pFileContext, fileAttributes, replaceFileAttributes, allocationSize, pFileInfo) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                FileInfo fi = winfsp.overwrite(
                        ctx,
                        FileAttributes.setOf(fileAttributes),
                        bool(replaceFileAttributes),
                        allocationSize
                );

                putFileInfo(pFileInfo, fi);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "Overwrite", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "Overwrite", ctx);
                throw e;
            }
        });
    }

    void initCleanup(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Cleanup.set((pFS, pFileContext, _pFileName, flags) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                EnumSet<CleanupFlags> cleanupFlags = CleanupFlags.setOf(flags);
                winfsp.cleanup(
                        ctx,
                        cleanupFlags
                );
            }
            catch (Throwable e) {
                logError(e, "Cleanup", ctx);
                throw e;
            }
        });
    }

    void initClose(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Close.set((pFS, pFileContext) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                winfsp.close(ctx);
            }
            catch (Throwable e) {
                logError(e, "Close", ctx);
                throw e;
            }
            finally {
                openContexts.remove(ctxKey(pFileContext));
            }
        });
    }

    void initRead(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Read.set((pFS, pFileContext, pBuffer, offset, length, pBytesTransferred) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                long bytesTransferred = winfsp.read(
                        ctx,
                        pBuffer,
                        offset,
                        length
                );

                pBytesTransferred.putLong(0, bytesTransferred);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "Read", ctx);
                // TODO handle STATUS_PENDING(0x103) for async operations
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "Read", ctx);
                throw e;
            }
        });
    }

    void initWrite(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Write.set((pFS, pFileContext, pBuffer, offset, length, writeToEndOfFile, constrainedIo,
                       pBytesTransferred, pFileInfo) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                WriteResult res = winfsp.write(
                        ctx,
                        pBuffer,
                        offset,
                        length,
                        bool(writeToEndOfFile),
                        bool(constrainedIo)
                );

                if (!(bool(constrainedIo) && res.getBytesTransferred() == 0))
                    pBytesTransferred.putLong(0, res.getBytesTransferred());

                putFileInfo(pFileInfo, res.getFileInfo());

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "Write", ctx);
                // TODO handle STATUS_PENDING(0x103) for async operations
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "Write", ctx);
                throw e;
            }
        });
    }

    void initFlush(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Flush.set((pFS, pFileContext, pFileInfo) -> {

            OpenContext ctx = null;
            try {
                ctx = (pFileContext == null) ? null : ctxValue(pFileContext);
                FileInfo fi = winfsp.flush(ctx);

                if (ctx != null && fi != null) {
                    putFileInfo(pFileInfo, fi);
                }

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "Flush", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "Flush", ctx);
                throw e;
            }
        });
    }

    void initGetFileInfo(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetFileInfo.set((pFS, pFileContext, pFileInfo) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                FileInfo fi = winfsp.getFileInfo(ctx);

                putFileInfo(pFileInfo, fi);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "GetFileInfo", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "GetFileInfo", ctx);
                throw e;
            }
        });
    }

    void initSetBasicInfo(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetBasicInfo.set((pFS, pFileContext, fileAttributes, creationTime, lastAccessTime, lastWriteTime, changeTime,
                              pFileInfo) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                FileInfo fi = winfsp.setBasicInfo(
                        ctx,
                        FileAttributes.setOf(fileAttributes),
                        new WinSysTime(creationTime),
                        new WinSysTime(lastAccessTime),
                        new WinSysTime(lastWriteTime),
                        new WinSysTime(changeTime)
                );

                putFileInfo(pFileInfo, fi);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "SetBasicInfo", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "SetBasicInfo", ctx);
                throw e;
            }
        });
    }

    void initSetFileSize(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetFileSize.set((pFS, pFileContext, newSize, setAllocationSize, pFileInfo) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                FileInfo res = winfsp.setFileSize(
                        ctx,
                        newSize,
                        bool(setAllocationSize)
                );

                putFileInfo(pFileInfo, res);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "SetFileSize", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "SetFileSize", ctx);
                throw e;
            }
        });
    }

    void initCanDelete(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.CanDelete.set((pFS, pFileContext, _pFileName) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                winfsp.canDelete(ctx);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "CanDelete", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "CanDelete", ctx);
                throw e;
            }
        });
    }

    void initRename(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Rename.set((pFS, pFileContext, pFileName, pNewFileName, replaceIfExists) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                String newFileName = StringUtils.fromPointer(pNewFileName);

                winfsp.rename(
                        ctx,
                        StringUtils.fromPointer(pFileName),
                        newFileName,
                        bool(replaceIfExists)
                );

                ctx.setPath(newFileName);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "Rename", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "Rename", ctx);
                throw e;
            }
        });
    }

    void initGetSecurity(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetSecurity.set((pFS, pFileContext, pSecurityDescriptor, pSecurityDescriptorSize) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                byte[] securityDescriptor = winfsp.getSecurity(ctx);

                SecurityDescriptorUtils.fromBytes(
                        RUNTIME,
                        securityDescriptor,
                        pSecurityDescriptor,
                        pSecurityDescriptorSize
                );

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "GetSecurity", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "GetSecurity", ctx);
                throw e;
            }
        });
    }

    void initSetSecurity(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetSecurity.set((pFS, pFileContext, securityInformation, pModificationDescriptor) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);

                byte[] securityDescriptor = winfsp.getSecurity(ctx);
                byte[] modifiedSecurityDescriptor = SecurityDescriptorUtils.modify(
                        RUNTIME,
                        securityDescriptor,
                        securityInformation,
                        pModificationDescriptor
                );

                winfsp.setSecurity(ctx, modifiedSecurityDescriptor);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "SetSecurity", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "SetSecurity", ctx);
                throw e;
            }
        });
    }

    void initReadDirectory(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.ReadDirectory.set((pFS, pFileContext, pPattern, pMarker, pBuffer, length, pBytesTransferred) -> {

            OpenContext ctx = null;
            try {
                String pattern = StringUtils.fromPointer(pPattern);
                String marker = StringUtils.fromPointer(pMarker);

                class MutableBoolean {
                    boolean bool;
                }
                MutableBoolean allAdded = new MutableBoolean();
                allAdded.bool = true;

                ctx = ctxValue(pFileContext);
                winfsp.readDirectory(
                        ctx,
                        pattern,
                        marker,
                        (fi) -> {
                            if (!allAdded.bool)
                                return false;

                            String fileName = fi.getFileName();
                            byte[] fileNameBytes = StringUtils.toBytes(fileName, false);

                            Pointered<FSP_FSCTL_DIR_INFO> diP = FSP_FSCTL_DIR_INFO.create(fileNameBytes.length);
                            _putDirInfo(diP.get(), fi, fileNameBytes);

                            byte added = LibWinFsp.INSTANCE.FspFileSystemAddDirInfo(
                                    diP.getPointer(),
                                    pBuffer,
                                    length,
                                    pBytesTransferred
                            );
                            diP.free(); // avoid memory leak

                            allAdded.bool &= bool(added);

                            return bool(added);
                        }
                );

                // add one final null entry to mark the end of the operation
                if (allAdded.bool)
                    LibWinFsp.INSTANCE.FspFileSystemAddDirInfo(null, pBuffer, length, pBytesTransferred);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "ReadDirectory", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "ReadDirectory", ctx);
                throw e;
            }
        });
    }

    void initResolveReparsePoints(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.ResolveReparsePoints.set((pFS, pFileName, reparsePointIndex, resolveLastPathComponent, pIoStatus,
                                      pBuffer, pSize) -> {
            return LibWinFsp.INSTANCE.FspFileSystemResolveReparsePoints(
                    pFS,
                    this.getReparsePointCallback,
                    null,
                    pFileName,
                    reparsePointIndex,
                    resolveLastPathComponent,
                    pIoStatus,
                    pBuffer,
                    pSize
            );
        });
    }

    void initGetReparsePoint(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetReparsePoint.set((pFS, pFileContext, _pFileName, pBuffer, pSize) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                byte[] reparseData = winfsp.getReparsePointData(ctx);

                if (reparseData.length > pSize.getLong(0))
                    throw new NTStatusException(0xC0000023); // STATUS_BUFFER_TOO_SMALL

                pSize.putLong(0, reparseData.length);
                pBuffer.put(0, reparseData, 0, reparseData.length);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "GetReparsePoint", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "GetReparsePoint", ctx);
                throw e;
            }
        });
    }

    void initSetReparsePoint(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetReparsePoint.set((pFS, pFileContext, _pFileName, pBuffer, size) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                ensureReparsePointCanBeReplaced(ctx, pBuffer, size);

                byte[] replaceReparseData = PointerUtils.getBytes(pBuffer, 0, (int) size);
                int reparseTag = pBuffer.getInt(0); /* the first field in a reparse buffer is the reparse tag */
                winfsp.setReparsePoint(ctx, replaceReparseData, reparseTag);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "SetReparsePoint", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "SetReparsePoint", ctx);
                throw e;
            }
        });
    }

    void initDeleteReparsePoint(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.DeleteReparsePoint.set((pFS, pFileContext, _pFileName, pBuffer, size) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                ensureReparsePointCanBeReplaced(ctx, pBuffer, size);

                winfsp.deleteReparsePoint(ctx);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "DeleteReparsePoint", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "DeleteReparsePoint", ctx);
                throw e;
            }
        });
    }

    private LibWinFsp.GetReparsePointByNameCallback newGetReparsePointByNameCallback() {
        return ((pFS, _pContext, pFileName, isDirectory, _pBuffer, _pSize) -> {

            OpenContext ctx = null;
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                ctx = bool(isDirectory)
                        ? OpenContext.newDirectoryContext(0L, fileName)
                        : OpenContext.newFileContext(0L, fileName);

                winfsp.getReparsePointData(ctx);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "GetReparsePointByName", ctx);
                return e.getNtStatus();
            }
            catch (Throwable e) {
                logError(e, "GetReparsePointByName", ctx);
                throw e;
            }
        });
    }

    private void ensureReparsePointCanBeReplaced(OpenContext ctx,
                                                 Pointer /* VOID */ pReplaceReparseData,
                                                 @size_t long replaceReparseDataSize) throws NTStatusException {

        byte[] data = winfsp.getReparsePointData(ctx);
        Pointer pCurrentReparseData = pointerFromBytes(data);
        int status = LibWinFsp.INSTANCE.FspFileSystemCanReplaceReparsePoint(
                pCurrentReparseData,
                data.length,
                pReplaceReparseData,
                replaceReparseDataSize
        );
        PointerUtils.freeMemory(pCurrentReparseData);

        if (status != 0)
            throw new NTStatusException(status);
    }

    void initGetDirInfoByName(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetDirInfoByName.set((pFS, pFileContext, pFileName, pDirInfo) -> {

            OpenContext ctx = null;
            try {
                ctx = ctxValue(pFileContext);
                FileInfo fi = winfsp.getDirInfoByName(
                        ctx,
                        StringUtils.fromPointer(pFileName)
                );

                String fileName = fi.getFileName();
                byte[] fileNameBytes = StringUtils.getEncoder().reset().encode(CharBuffer.wrap(fileName)).array();

                Pointered<FSP_FSCTL_DIR_INFO> diP = FSP_FSCTL_DIR_INFO.of(pDirInfo, fileNameBytes.length);
                _putDirInfo(diP.get(), fi, fileNameBytes);

                return 0;
            }
            catch (NTStatusException e) {
                logError(e, "GetDirInfoByName", ctx);
                return e.getNtStatus();
            }
            catch (CharacterCodingException cce) {
                logError(cce, "GetDirInfoByName", ctx);
                throw new RuntimeException(cce);
            }
            catch (Throwable e) {
                logError(e, "GetDirInfoByName", ctx);
                throw e;
            }
        });
    }

    private OpenContext ctxValue(Pointer pFileContext) {
        return openContexts.get(ctxKey(pFileContext));
    }

    private long ctxKey(Pointer pFileContext) {
        return pFileContext.address();
    }

    private void putFileContext(Pointer ppFileContext, OpenResult res) throws NTStatusException {
        boolean isDirectory = res.getFileInfo().getFileAttributes().contains(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);

        long handle = res.getFileHandle();
        if (handle == 0L || (int) handle == 0) {
            // ensure we never put a 0 address, either in 32-bit or 64-bit arch
            throw new NTStatusException(0xC00000E5); // STATUS_INTERNAL_ERROR
        }

        OpenContext ctx = isDirectory
                ? OpenContext.newDirectoryContext(handle, res.getFileInfo().getFileName())
                : OpenContext.newFileContext(handle, res.getFileInfo().getFileName());

        openContexts.put(handle, ctx);
        ppFileContext.putAddress(0, handle);
    }

    private void logError(Throwable ex, String op) {
        logError(ex, op, (String) null);
    }

    private void logError(Throwable ex, String op, OpenContext ctx) {
        logError(ex, op, ctx != null ? ctx.getPath() : null);
    }

    private void logError(Throwable ex, String op, String path) {
        if (this.verboseErr != null) {
            verboseErr.printf(
                    "%s [%s] ERROR - %s - %s%s%n",
                    ERROR_TIME_FORMATTER.format(LocalDateTime.now()),
                    Thread.currentThread().getName(),
                    op,
                    getErrorDetail(ex),
                    path != null ? (" - " + path) : ""
            );
            ex.printStackTrace(verboseErr);
        }
    }

    private static String getErrorDetail(Throwable ex) {
        return ex instanceof NTStatusException
                ? String.format("0x%08x", ((NTStatusException) ex).getNtStatus())
                : String.valueOf(ex.getMessage());
    }

    private static boolean bool(byte val) {
        return PointerUtils.BOOLEAN(val);
    }

    private static void putOpenFileInfo(Pointer pOFI, FileInfo fi) {
        FSP_FSCTL_OPEN_FILE_INFO ofiOut = FSP_FSCTL_OPEN_FILE_INFO.of(pOFI).get();
        _putFileInfo(ofiOut.FileInfo, fi);
        Pointer namePointer = StringUtils.toPointer(pOFI.getRuntime(), fi.getNormalizedName(), true);
        ofiOut.NormalizedName.get().transferFrom(0, namePointer, 0, namePointer.size());
        ofiOut.NormalizedNameSize.set(namePointer.size());
        StringUtils.freeStringPointer(namePointer);
    }

    private static void putFileInfo(Pointer pFI, FileInfo fi) {
        FSP_FSCTL_FILE_INFO fiOut = FSP_FSCTL_FILE_INFO.of(pFI).get();
        _putFileInfo(fiOut, fi);
    }

    private static void _putDirInfo(FSP_FSCTL_DIR_INFO diOut, FileInfo fi, byte[] fileNameBytes) {
        diOut.Size.set(Struct.size(diOut)); // size already includes file name length
        _putFileInfo(diOut.FileInfo, fi);
        diOut.setFileName(fileNameBytes);
    }

    private static void _putFileInfo(FSP_FSCTL_FILE_INFO fiOut, FileInfo fi) {
        fiOut.FileAttributes.set(FileAttributes.intOf(fi.getFileAttributes()));
        fiOut.ReparseTag.set(fi.getReparseTag());
        fiOut.AllocationSize.set(fi.getAllocationSize());
        fiOut.FileSize.set(fi.getFileSize());
        fiOut.CreationTime.set(fi.getCreationTime().get());
        fiOut.LastAccessTime.set(fi.getLastAccessTime().get());
        fiOut.LastWriteTime.set(fi.getLastWriteTime().get());
        fiOut.ChangeTime.set(fi.getChangeTime().get());
        fiOut.IndexNumber.set(fi.getIndexNumber());
        fiOut.HardLinks.set(fi.getHardLinks());
        fiOut.EaSize.set(fi.getEaSize());
    }

    private static Pointer pointerFromBytes(byte[] bytes) {
        Pointer p = PointerUtils.allocateMemory(RUNTIME, bytes.length);
        p.put(0, bytes, 0, bytes.length);
        return p;
    }
}
