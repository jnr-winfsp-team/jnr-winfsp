package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.flags.CleanupFlags;
import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.lib.LibWinFsp;
import com.github.jnrwinfspteam.jnrwinfsp.lib.StringUtils;
import com.github.jnrwinfspteam.jnrwinfsp.result.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.*;
import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import com.github.jnrwinfspteam.jnrwinfsp.util.WinSysTime;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.EnumSet;
import java.util.List;

final class FSHelper {
    static void initGetVolumeInfo(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.GetVolumeInfo.set((pFS, pVolumeInfo) -> {
            try {
                VolumeInfo vi = winfsp.getVolumeInfo(fs(pFS));
                FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                viOut.TotalSize.set(vi.getTotalSize());
                viOut.FreeSize.set(vi.getFreeSize());
                viOut.setVolumeLabel(vi.getVolumeLabel());

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initSetVolumeLabel(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.SetVolumeLabel.set((pFS, pVolumeLabel, pVolumeInfo) -> {

            try {
                VolumeInfo vi = winfsp.setVolumeLabel(fs(pFS), StringUtils.fromPointer(pVolumeLabel));

                FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                viOut.TotalSize.set(vi.getTotalSize());
                viOut.FreeSize.set(vi.getFreeSize());
                viOut.setVolumeLabel(vi.getVolumeLabel());

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initGetSecurityByName(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.GetSecurityByName.set((pFS, pFileName, pFileAttributes, pSecurityDescriptor, pSecurityDescriptorSize) -> {
            ResultSecurityAndAttributes res = winfsp.getSecurityByName(fs(pFS), StringUtils.fromPointer(pFileName));

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

    static void initCreate(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Create.set((pFS, pFileName, createOptions, grantedAccess, fileAttributes,
                        pSecurityDescriptor, allocationSize, ppFileContext, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                FileInfo fi = winfsp.create(
                        fs(pFS),
                        fileName,
                        CreateOptions.setValueOf(createOptions),
                        grantedAccess,
                        FileAttributes.setValueOf(fileAttributes),
                        pSecurityDescriptor,
                        allocationSize
                );

                putFileInfo(pFileInfo, fi);
                putFileContext(ppFileContext, fileName);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initOpen(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Open.set((pFS, pFileName, createOptions, grantedAccess, ppFileContext, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                FileInfo fi = winfsp.open(
                        fs(pFS),
                        fileName,
                        CreateOptions.setValueOf(createOptions),
                        grantedAccess
                );

                putFileInfo(pFileInfo, fi);
                putFileContext(ppFileContext, fileName);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initOverwrite(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Overwrite.set((pFS, pFileContext, fileAttributes, replaceFileAttributes, allocationSize, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.overwrite(
                        fs(pFS),
                        fileName,
                        FileAttributes.setValueOf(fileAttributes),
                        replaceFileAttributes,
                        allocationSize
                );

                putFileInfo(pFileInfo, fi);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initCleanup(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Cleanup.set((pFS, pFileContext, pFileName, flags) -> {
            String fileName = StringUtils.fromPointer(pFileName);
            EnumSet<CleanupFlags> cleanupFlags = CleanupFlags.setValueOf(Math.toIntExact(flags));
            winfsp.cleanup(
                    fs(pFS),
                    fileName,
                    cleanupFlags
            );
        });
    }

    static void initClose(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Close.set((pFS, pFileContext) -> {
            String fileName = StringUtils.fromPointer(pFileContext);
            winfsp.close(fs(pFS), fileName);
            StringUtils.freeStringPointer(pFileContext);
        });
    }

    static void initRead(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Read.set((pFS, pFileContext, pBuffer, offset, length, pBytesTransferred) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                long bytesTransferred = winfsp.read(fs(pFS), fileName, pBuffer, offset, length);

                pBytesTransferred.putLong(0, bytesTransferred);

                return 0;
            } catch (NTStatusException e) {
                // TODO handle STATUS_PENDING(0x103) for async operations
                return e.getNtStatus();
            }
        });
    }

    static void initWrite(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Write.set((pFS, pFileContext, pBuffer, offset, length, writeToEndOfFile, constrainedIo,
                       pBytesTransferred, pFileInfo) -> {
            try {
                FSP_FILE_SYSTEM fs = fs(pFS);
                String fileName = StringUtils.fromPointer(pFileContext);
                long bytesTransferred = winfsp.write(
                        fs,
                        fileName,
                        pBuffer,
                        offset,
                        length,
                        writeToEndOfFile,
                        constrainedIo
                );
                FileInfo fi = winfsp.getFileInfo(fs, fileName);

                pBytesTransferred.putLong(0, bytesTransferred);
                putFileInfo(pFileInfo, fi);

                return 0;
            } catch (NTStatusException e) {
                // TODO handle STATUS_PENDING(0x103) for async operations
                return e.getNtStatus();
            }
        });
    }

    static void initFlush(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Flush.set((pFS, pFileContext, pFileInfo) -> {
            try {
                FSP_FILE_SYSTEM fs = fs(pFS);
                String fileName = StringUtils.fromPointer(pFileContext);
                winfsp.flush(fs, fileName);

                if (fileName != null) {
                    FileInfo fi = winfsp.getFileInfo(fs, fileName);
                    putFileInfo(pFileInfo, fi);
                }

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initGetFileInfo(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.GetFileInfo.set((pFS, pFileContext, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.getFileInfo(fs(pFS), fileName);

                putFileInfo(pFileInfo, fi);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initSetBasicInfo(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.SetBasicInfo.set((pFS, pFileContext, fileAttributes, creationTime, lastAccessTime, lastWriteTime, changeTime,
                              pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo fi = winfsp.setBasicInfo(
                        fs(pFS),
                        fileName,
                        FileAttributes.setValueOf(fileAttributes),
                        new WinSysTime(creationTime),
                        new WinSysTime(lastAccessTime),
                        new WinSysTime(lastWriteTime),
                        new WinSysTime(changeTime)
                );

                putFileInfo(pFileInfo, fi);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initSetFileSize(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.SetFileSize.set((pFS, pFileContext, newSize, setAllocationSize, pFileInfo) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileContext);
                FileInfo res = winfsp.setFileSize(fs(pFS), fileName, newSize, setAllocationSize);

                putFileInfo(pFileInfo, res);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initCanDelete(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.CanDelete.set((pFS, _pFileContext, pFileName) -> {
            try {
                String fileName = StringUtils.fromPointer(pFileName);
                winfsp.canDelete(fs(pFS), fileName);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initRename(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Rename.set((pFS, _pFileContext, pFileName, pNewFileName, replaceIfExists) -> {
            try {
                winfsp.rename(
                        fs(pFS),
                        StringUtils.fromPointer(pFileName),
                        StringUtils.fromPointer(pNewFileName),
                        replaceIfExists
                );

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            }
        });
    }

    static void initGetSecurity(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.GetSecurity.set((pFS, pFileContext, pSecurityDescriptor, pSecurityDescriptorSize) -> {
            String fileName = StringUtils.fromPointer(pFileContext);
            ResultSecurity res = winfsp.getSecurity(fs(pFS), fileName);

            if (res.getNtStatus() == 0) {
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

    static void initSetSecurity(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.SetSecurity.set((pFS, pFileContext, securityInformation, pModificationDescriptor) -> {
            String fileName = StringUtils.fromPointer(pFileContext);
            Result res = winfsp.setSecurity(fs(pFS), fileName, securityInformation, pModificationDescriptor);
            return res.getNtStatus();
        });
    }

    static void initReadDirectory(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp, LibWinFsp libWinFsp) {
        fsi.ReadDirectory.set((pFS, pFileContext, pPattern, pMarker, pBuffer, length, pBytesTransferred) -> {
            try {
                List<FileInfo> fileInfos = winfsp.readDirectory(
                        fs(pFS),
                        StringUtils.fromPointer(pFileContext),
                        StringUtils.fromPointer(pPattern),
                        StringUtils.fromPointer(pMarker)
                );

                for (var fi : fileInfos) {
                    String fileName = fi.getFileName();
                    byte[] fileNameBytes = StringUtils.getEncoder().reset().encode(CharBuffer.wrap(fileName)).array();

                    Pointered<FSP_FSCTL_DIR_INFO> diP = FSP_FSCTL_DIR_INFO.create(fileNameBytes.length);
                    FSP_FSCTL_DIR_INFO di = diP.get();
                    di.Size.set(Struct.size(di)); // size already includes file name length
                    di.FileInfo.FileAttributes.set(FileAttributes.intValueOf(fi.getFileAttributes()));
                    di.FileInfo.ReparseTag.set(fi.getReparseTag());
                    di.FileInfo.AllocationSize.set(fi.getAllocationSize());
                    di.FileInfo.FileSize.set(fi.getFileSize());
                    di.FileInfo.CreationTime.set(fi.getCreationTime().get());
                    di.FileInfo.LastAccessTime.set(fi.getLastAccessTime().get());
                    di.FileInfo.LastWriteTime.set(fi.getLastWriteTime().get());
                    di.FileInfo.ChangeTime.set(fi.getChangeTime().get());
                    di.FileInfo.IndexNumber.set(fi.getIndexNumber());
                    di.FileInfo.HardLinks.set(fi.getHardLinks());
                    di.FileInfo.EaSize.set(fi.getEaSize());
                    di.setFileName(fileNameBytes);

                    boolean added = libWinFsp.FspFileSystemAddDirInfo(
                            diP.getPointer(),
                            pBuffer,
                            length,
                            pBytesTransferred
                    );
                    if (!added)
                        return 0; // abort but with no error
                }

                // add one final null entry to mark the end of the operation
                libWinFsp.FspFileSystemAddDirInfo(null, pBuffer, length, pBytesTransferred);

                return 0;
            } catch (NTStatusException e) {
                return e.getNtStatus();
            } catch (CharacterCodingException cce) {
                throw new RuntimeException(cce);
            }
        });
    }

    private static FSP_FILE_SYSTEM fs(Pointer pFS) {
        return FSP_FILE_SYSTEM.of(pFS).get();
    }

    private static void putFileInfo(Pointer pFI, FileInfo fi) {
        FSP_FSCTL_FILE_INFO fiOut = FSP_FSCTL_FILE_INFO.of(pFI).get();
        fiOut.FileAttributes.set(FileAttributes.intValueOf(fi.getFileAttributes()));
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
        Pointer p = StringUtils.toPointer(Runtime.getSystemRuntime(), fileName);
        ppFileContext.putPointer(0, p);
    }
}
