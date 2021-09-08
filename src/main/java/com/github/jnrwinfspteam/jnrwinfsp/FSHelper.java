package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.api.*;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.*;
import com.github.jnrwinfspteam.jnrwinfsp.internal.struct.*;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.PointerUtils;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.SecurityUtils;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.StringUtils;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.Pointered;
import com.github.jnrwinfspteam.jnrwinfsp.api.WinSysTime;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.types.size_t;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class FSHelper {

    private static final Runtime RUNTIME = Runtime.getSystemRuntime();

    private final WinFspFS winfsp;
    private final LibWinFsp libWinFsp;
    private final LibKernel32 libKernel32;
    private final LibAdvapi32 libAdvapi32;

    private final PrintStream verboseErr;

    FSHelper(WinFspFS winfsp, LibWinFsp libWinFsp, LibKernel32 libKernel32, LibAdvapi32 libAdvapi32, boolean debug) {
        this.winfsp = Objects.requireNonNull(winfsp);
        this.libWinFsp = Objects.requireNonNull(libWinFsp);
        this.libKernel32 = Objects.requireNonNull(libKernel32);
        this.libAdvapi32 = Objects.requireNonNull(libAdvapi32);

        this.verboseErr = debug ? System.err : new PrintStream(OutputStream.nullOutputStream());
    }

    void initGetVolumeInfo(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetVolumeInfo.set((pFS, pVolumeInfo) -> {
            try {
                VolumeInfo vi = winfsp.getVolumeInfo(fs(pFS));
                FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                viOut.TotalSize.set(vi.getTotalSize());
                viOut.FreeSize.set(vi.getFreeSize());
                viOut.setVolumeLabel(vi.getVolumeLabel());

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR GetVolumeInfo: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initSetVolumeLabel(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetVolumeLabel.set((pFS, pVolumeLabel, pVolumeInfo) -> {

            try {
                VolumeInfo vi = winfsp.setVolumeLabel(fs(pFS), StringUtils.fromPointer(pVolumeLabel));

                FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                viOut.TotalSize.set(vi.getTotalSize());
                viOut.FreeSize.set(vi.getFreeSize());
                viOut.setVolumeLabel(vi.getVolumeLabel());

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR SetVolumeLabel: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initGetSecurityByName(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetSecurityByName.set((pFS, pFileName, pFileAttributes, pSecurityDescriptor, pSecurityDescriptorSize) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                Optional<SecurityResult> opSR = winfsp.getSecurityByName(fs(pFS), fileName);
                if (opSR.isEmpty()) {
                    byte res = libWinFsp.FspFileSystemFindReparsePoint(
                            pFS,
                            newGetReparsePointByNameCallback(),
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

                SecurityUtils.fromString(
                        libWinFsp,
                        libKernel32,
                        libAdvapi32,
                        sr.getSecurityDescriptor(),
                        pSecurityDescriptor,
                        pSecurityDescriptorSize
                );

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR GetSecurityByName: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initCreateEx(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.CreateEx.set((pFS, pFileName, createOptions, grantedAccess, fileAttributes, pSecurityDescriptor, allocationSize,
                          pExtraBuffer, extraLength, extraBufferIsReparsePoint,
                          ppFileContext, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                String securityDescriptorStr = SecurityUtils.toString(
                        libWinFsp,
                        libKernel32,
                        libAdvapi32,
                        pSecurityDescriptor
                );

                ReparsePoint reparsePoint = null;
                if (pExtraBuffer != null && bool(extraBufferIsReparsePoint)) {
                    byte[] reparsePointData = PointerUtils.getBytes(pExtraBuffer, 0, (int) extraLength);
                    int reparseTag = pExtraBuffer.getInt(0); /* the first field in a reparse buffer is the tag */
                    reparsePoint = new ReparsePoint(reparsePointData, reparseTag);
                }

                FileInfo fi = winfsp.create(
                        fs(pFS),
                        fileName,
                        CreateOptions.setOf(createOptions),
                        grantedAccess,
                        FileAttributes.setOf(fileAttributes),
                        securityDescriptorStr,
                        allocationSize,
                        reparsePoint
                );

                putOpenFileInfo(pFileInfo, fi);
                putFileContext(ppFileContext, fi.getFileName());

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR CreateEx: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initOpen(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Open.set((pFS, pFileName, createOptions, grantedAccess, ppFileContext, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                FileInfo fi = winfsp.open(
                        fs(pFS),
                        fileName,
                        CreateOptions.setOf(createOptions),
                        grantedAccess
                );

                putOpenFileInfo(pFileInfo, fi);
                putFileContext(ppFileContext, fi.getFileName());

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR Open: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initOverwrite(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Overwrite.set((pFS, pFileContext, fileAttributes, replaceFileAttributes, allocationSize, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.overwrite(
                        fs(pFS),
                        fileName,
                        FileAttributes.setOf(fileAttributes),
                        bool(replaceFileAttributes),
                        allocationSize
                );

                putFileInfo(pFileInfo, fi);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR Overwrite: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initCleanup(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Cleanup.set((pFS, pFileContext, _pFileName, flags) -> {
            String fileName = StringUtils.fromPointer(pFileContext);
            EnumSet<CleanupFlags> cleanupFlags = CleanupFlags.setOf(flags);
            winfsp.cleanup(
                    fs(pFS),
                    fileName,
                    cleanupFlags
            );
        });
    }

    void initClose(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Close.set((pFS, pFileContext) -> {
            String fileName = StringUtils.fromPointer(pFileContext);
            winfsp.close(fs(pFS), fileName);
            StringUtils.freeStringPointer(pFileContext);
        });
    }

    void initRead(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Read.set((pFS, pFileContext, pBuffer, offset, length, pBytesTransferred) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                long bytesTransferred = winfsp.read(fs(pFS), fileName, pBuffer, offset, length);

                pBytesTransferred.putLong(0, bytesTransferred);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR Read: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                // TODO handle STATUS_PENDING(0x103) for async operations
                return e.getNtStatus();
            }
        });
    }

    void initWrite(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Write.set((pFS, pFileContext, pBuffer, offset, length, writeToEndOfFile, constrainedIo,
                       pBytesTransferred, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                WriteResult res = winfsp.write(
                        fs(pFS),
                        fileName,
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
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR Write: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                // TODO handle STATUS_PENDING(0x103) for async operations
                return e.getNtStatus();
            }
        });
    }

    void initFlush(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Flush.set((pFS, pFileContext, pFileInfo) -> {
            try {
                FSP_FILE_SYSTEM fs = fs(pFS);
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.flush(fs, fileName);

                if (fileName != null && fi != null) {
                    putFileInfo(pFileInfo, fi);
                }

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR Flush: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initGetFileInfo(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetFileInfo.set((pFS, pFileContext, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.getFileInfo(fs(pFS), fileName);

                putFileInfo(pFileInfo, fi);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR GetFileInfo: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initSetBasicInfo(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetBasicInfo.set((pFS, pFileContext, fileAttributes, creationTime, lastAccessTime, lastWriteTime, changeTime,
                              pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.setBasicInfo(
                        fs(pFS),
                        fileName,
                        FileAttributes.setOf(fileAttributes),
                        new WinSysTime(creationTime),
                        new WinSysTime(lastAccessTime),
                        new WinSysTime(lastWriteTime),
                        new WinSysTime(changeTime)
                );

                putFileInfo(pFileInfo, fi);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR SetBasicInfo: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initSetFileSize(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetFileSize.set((pFS, pFileContext, newSize, setAllocationSize, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo res = winfsp.setFileSize(fs(pFS), fileName, newSize, bool(setAllocationSize));

                putFileInfo(pFileInfo, res);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR SetFileSize: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initCanDelete(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.CanDelete.set((pFS, _pFileContext, pFileName) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                winfsp.canDelete(fs(pFS), fileName);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR CanDelete: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initRename(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.Rename.set((pFS, _pFileContext, pFileName, pNewFileName, replaceIfExists) -> {
            try {
                winfsp.rename(
                        fs(pFS),
                        StringUtils.fromPointer(pFileName),
                        StringUtils.fromPointer(pNewFileName),
                        bool(replaceIfExists)
                );

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR Rename: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initGetSecurity(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.GetSecurity.set((pFS, pFileContext, pSecurityDescriptor, pSecurityDescriptorSize) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                String securityDescriptorStr = winfsp.getSecurity(fs(pFS), fileName);

                SecurityUtils.fromString(
                        libWinFsp,
                        libKernel32,
                        libAdvapi32,
                        securityDescriptorStr,
                        pSecurityDescriptor,
                        pSecurityDescriptorSize
                );

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR GetSecurity: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initSetSecurity(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetSecurity.set((pFS, pFileContext, securityInformation, pModificationDescriptor) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                String securityDescriptorStr = winfsp.getSecurity(fs(pFS), fileName);
                String modifiedSecurityDescriptorStr = SecurityUtils.modify(
                        libWinFsp,
                        libKernel32,
                        libAdvapi32,
                        securityDescriptorStr,
                        securityInformation,
                        pModificationDescriptor
                );

                winfsp.setSecurity(fs(pFS), fileName, modifiedSecurityDescriptorStr);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR SetSecurity: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initReadDirectory(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.ReadDirectory.set((pFS, pFileContext, pPattern, pMarker, pBuffer, length, pBytesTransferred) -> {
            try {
                String pattern = StringUtils.fromPointer(pPattern);
                String marker = StringUtils.fromPointer(pMarker);
                List<FileInfo> fileInfos = winfsp.readDirectory(
                        fs(pFS),
                        StringUtils.fromPointer(pFileContext),
                        pattern,
                        marker
                );

                for (var fi : fileInfos) {
                    String fileName = fi.getFileName();
                    byte[] fileNameBytes = StringUtils.toBytes(fileName, false);

                    Pointered<FSP_FSCTL_DIR_INFO> diP = FSP_FSCTL_DIR_INFO.create(fileNameBytes.length);
                    _putDirInfo(diP.get(), fi, fileNameBytes);

                    byte added = libWinFsp.FspFileSystemAddDirInfo(
                            diP.getPointer(),
                            pBuffer,
                            length,
                            pBytesTransferred
                    );
                    diP.free(); // avoid memory leak
                    if (!bool(added))
                        return 0; // abort but with no error
                }

                // add one final null entry to mark the end of the operation
                libWinFsp.FspFileSystemAddDirInfo(null, pBuffer, length, pBytesTransferred);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR ReadDirectory: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initResolveReparsePoints(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.ResolveReparsePoints.set((pFS, pFileName, reparsePointIndex, resolveLastPathComponent, pIoStatus,
                                      pBuffer, pSize) -> {
            return libWinFsp.FspFileSystemResolveReparsePoints(
                    pFS,
                    newGetReparsePointByNameCallback(),
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
        fsi.GetReparsePoint.set((pFS, _pFileContext, pFileName, pBuffer, pSize) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                byte[] reparseData = winfsp.getReparsePointData(fs(pFS), fileName);

                if (reparseData.length > pSize.getLong(0))
                    throw new NTStatusException(0xC0000023); // STATUS_BUFFER_TOO_SMALL

                pSize.putLong(0, reparseData.length);
                pBuffer.put(0, reparseData, 0, reparseData.length);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR GetReparsePoint: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initSetReparsePoint(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.SetReparsePoint.set((pFS, _pFileContext, pFileName, pBuffer, size) -> {
            try {
                FSP_FILE_SYSTEM fs = fs(pFS);
                String fileName = StringUtils.fromPointer(pFileName);
                ensureReparsePointCanBeReplaced(fs, fileName, pBuffer, size);

                byte[] replaceReparseData = PointerUtils.getBytes(pBuffer, 0, (int) size);
                int reparseTag = pBuffer.getInt(0); /* the first field in a reparse buffer is the reparse tag */
                winfsp.setReparsePoint(fs, fileName, replaceReparseData, reparseTag);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR SetReparsePoint: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    void initDeleteReparsePoint(FSP_FILE_SYSTEM_INTERFACE fsi) {
        fsi.DeleteReparsePoint.set((pFS, _pFileContext, pFileName, pBuffer, size) -> {
            try {
                FSP_FILE_SYSTEM fs = fs(pFS);
                String fileName = StringUtils.fromPointer(pFileName);
                ensureReparsePointCanBeReplaced(fs, fileName, pBuffer, size);

                winfsp.deleteReparsePoint(fs, fileName);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR DeleteReparsePoint: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            }
        });
    }

    private LibWinFsp.GetReparsePointByNameCallback newGetReparsePointByNameCallback() {
        return ((pFS, _pContext, pFileName, _isDirectory, _pBuffer, _pSize) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                winfsp.getReparsePointData(fs(pFS), fileName);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    private void ensureReparsePointCanBeReplaced(FSP_FILE_SYSTEM fs,
                                                 String fileName,
                                                 Pointer /* VOID */ pReplaceReparseData,
                                                 @size_t long replaceReparseDataSize) throws NTStatusException {

        byte[] data = winfsp.getReparsePointData(fs, fileName);
        Pointer pCurrentReparseData = pointerFromBytes(data);
        int status = libWinFsp.FspFileSystemCanReplaceReparsePoint(
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
            try {
                String parentDirName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.getDirInfoByName(
                        fs(pFS),
                        parentDirName,
                        StringUtils.fromPointer(pFileName)
                );

                String fileName = fi.getFileName();
                byte[] fileNameBytes = StringUtils.getEncoder().reset().encode(CharBuffer.wrap(fileName)).array();

                Pointered<FSP_FSCTL_DIR_INFO> diP = FSP_FSCTL_DIR_INFO.of(pDirInfo, fileNameBytes.length);
                _putDirInfo(diP.get(), fi, fileNameBytes);

                return 0;
            } catch (NTStatusException e) {
                verboseErr.printf("--- ERROR GetDirInfoByName: %08x%n", e.getNtStatus());
                e.printStackTrace(verboseErr);
                return e.getNtStatus();
            } catch (CharacterCodingException cce) {
                verboseErr.printf("--- ERROR GetDirInfoByName: %s%n", cce);
                cce.printStackTrace(verboseErr);
                throw new RuntimeException(cce);
            }
        });
    }

    private static FSP_FILE_SYSTEM fs(Pointer pFS) {
        return FSP_FILE_SYSTEM.of(pFS).get();
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

    private static void putFileContext(Pointer ppFileContext, String fileName) {
        Pointer p = StringUtils.toPointer(RUNTIME, fileName, true);
        ppFileContext.putPointer(0, p);
    }

    private static Pointer pointerFromBytes(byte[] bytes) {
        Pointer p = PointerUtils.allocateMemory(RUNTIME, bytes.length);
        p.put(0, bytes, 0, bytes.length);
        return p;
    }
}
