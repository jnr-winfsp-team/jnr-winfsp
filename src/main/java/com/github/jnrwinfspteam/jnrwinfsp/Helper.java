package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.lib.WinFspCallbacks;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultFileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultFileInfoAndContext;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultSecurityAndAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultVolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM_INTERFACE;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_FILE_INFO;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_VOLUME_INFO;
import jnr.ffi.Pointer;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * This is a helper class for bridging the implementation of the {@link WinFspCallbacks} and the
 * implementation of the {@link WinFspFS} interface.
 */
final class Helper {
    private static final int MAX_VOLUME_LABEL_LENGTH = 32;
    private static final int MAX_FILE_LENGTH = 260;
    private static final Charset CS = initCharset();

    private static Charset initCharset() {
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
            return StandardCharsets.UTF_16LE;
        else
            return StandardCharsets.UTF_16BE;
    }

    static byte[] getPathBytes(Path path) {
        return getStringBytes(path == null ? null : path.toString());
    }

    static byte[] getStringBytes(String s) {
        if (s == null)
            return null;
        else
            return s.getBytes(CS);
    }

    static void initGetVolumeInfo(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
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

    static void initSetVolumeLabel(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.SetVolumeLabel.set((pFS, pVolumeLabel, pVolumeInfo) -> {
            ResultVolumeInfo res = winfsp.setVolumeLabel(fs(pFS), string(pVolumeLabel, MAX_VOLUME_LABEL_LENGTH));
            if (res.getNtStatus() == 0) {
                FSP_FSCTL_VOLUME_INFO viOut = FSP_FSCTL_VOLUME_INFO.of(pVolumeInfo).get();
                viOut.TotalSize.set(res.getTotalSize());
                viOut.FreeSize.set(res.getFreeSize());
                viOut.setVolumeLabel(res.getVolumeLabel());
            }

            return res.getNtStatus();
        });
    }

    static void initGetSecurityByName(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.GetSecurityByName.set((pFS, pFileName, pFileAttributes, pSecurityDescriptor, pSecurityDescriptorSize) -> {
            ResultSecurityAndAttributes res = winfsp.getSecurityByName(fs(pFS), string(pFileName, MAX_FILE_LENGTH));
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
            ResultFileInfoAndContext res = winfsp.create(
                    fs(pFS),
                    string(pFileName, MAX_FILE_LENGTH),
                    createOptions,
                    grantedAccess,
                    fileAttributes,
                    pSecurityDescriptor,
                    allocationSize
            );

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
                ppFileContext.putPointer(0, res.getFileContextP().getPointer());
            }

            return res.getNtStatus();
        });
    }

    static void initOpen(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Open.set((pFS, pFileName, createOptions, grantedAccess, ppFileContext, pFileInfo) -> {
            ResultFileInfoAndContext res = winfsp.open(
                    fs(pFS),
                    string(pFileName, MAX_FILE_LENGTH),
                    createOptions,
                    grantedAccess
            );

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
                ppFileContext.putPointer(0, res.getFileContextP().getPointer());
            }

            return res.getNtStatus();
        });
    }

    static void initOverwrite(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Overwrite.set((pFS, pFileContext, fileAttributes, replaceFileAttributes, allocationSize, pFileInfo) -> {
            ResultFileInfo res = winfsp.overwrite(
                    fs(pFS),
                    FileContext.of(pFileContext).get(),
                    fileAttributes,
                    replaceFileAttributes,
                    allocationSize
            );

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
            }

            return res.getNtStatus();
        });
    }

    static void initCleanup(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Cleanup.set((pFS, pFileContext, pFileName, flags) -> {
            winfsp.cleanup(
                    fs(pFS),
                    FileContext.of(pFileContext).get(),
                    string(pFileName, MAX_FILE_LENGTH),
                    flags
            );
        });
    }

    static void initClose(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Close.set((pFS, pFileContext) -> {
            winfsp.close(fs(pFS), FileContext.of(pFileContext).get());
        });
    }

    private static FSP_FILE_SYSTEM fs(Pointer pFS) {
        return FSP_FILE_SYSTEM.of(pFS).get();
    }

    private static String string(Pointer pStr, int maxLength) {
        if (pStr == null)
            return null;
        else
            return pStr.getString(0, maxLength, CS);
    }

    private static void putFileInfo(Pointer pFileInfo, ResultFileInfo res) {
        FSP_FSCTL_FILE_INFO fi = FSP_FSCTL_FILE_INFO.of(pFileInfo).get();
        fi.FileAttributes.set(res.getFileAttributes());
        fi.ReparseTag.set(res.getReparseTag());
        fi.AllocationSize.set(res.getAllocationSize());
        fi.FileSize.set(res.getFileSize());
        fi.CreationTime.set(res.getCreationTime());
        fi.LastAccessTime.set(res.getLastAccessTime());
        fi.LastWriteTime.set(res.getLastWriteTime());
        fi.ChangeTime.set(res.getChangeTime());
        fi.IndexNumber.set(res.getIndexNumber());
        fi.HardLinks.set(res.getHardLinks());
        fi.EaSize.set(res.getEaSize());
    }
}
