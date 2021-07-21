package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.result.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM_INTERFACE;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_FILE_INFO;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_VOLUME_INFO;
import jnr.ffi.Pointer;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.*;

final class FSHelper {
    private static final ThreadLocal<Reference<CharsetDecoder>> localDecoder = new ThreadLocal<>();
    private static final int terminatorLength = 2;
    private static final Charset CS;

    static {
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
            CS = StandardCharsets.UTF_16LE;
        else
            CS = StandardCharsets.UTF_16BE;
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
            ResultVolumeInfo res = winfsp.setVolumeLabel(fs(pFS), getString(pVolumeLabel));

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
            ResultSecurityAndAttributes res = winfsp.getSecurityByName(fs(pFS), getString(pFileName));

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
                    getString(pFileName),
                    createOptions,
                    grantedAccess,
                    fileAttributes,
                    pSecurityDescriptor,
                    allocationSize
            );

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
                putFileContext(ppFileContext, res);
            }

            return res.getNtStatus();
        });
    }

    static void initOpen(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Open.set((pFS, pFileName, createOptions, grantedAccess, ppFileContext, pFileInfo) -> {
            ResultFileInfoAndContext res = winfsp.open(fs(pFS), getString(pFileName), createOptions, grantedAccess);

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
                putFileContext(ppFileContext, res);
            }

            return res.getNtStatus();
        });
    }

    static void initOverwrite(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Overwrite.set((pFS, pFileContext, fileAttributes, replaceFileAttributes, allocationSize, pFileInfo) -> {
            ResultFileInfo res = winfsp.overwrite(
                    fs(pFS),
                    pFileContext,
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
            winfsp.cleanup(fs(pFS), pFileContext, getString(pFileName), flags);
        });
    }

    static void initClose(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Close.set((pFS, pFileContext) -> {
            winfsp.close(fs(pFS), pFileContext);
        });
    }

    static void initRead(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Read.set((pFS, pFileContext, pBuffer, offset, length, pBytesTransferred) -> {
            ResultRead res = winfsp.read(fs(pFS), pFileContext, pBuffer, offset, length);

            if (res.getNtStatus() == 0 || res.getNtStatus() == 0x103) {
                pBytesTransferred.putLong(0, res.getBytesTransferred());
            }

            return res.getNtStatus();
        });
    }

    static void initWrite(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Write.set((pFS, pFileContext, pBuffer, offset, length, writeToEndOfFile, constrainedIo,
                       pBytesTransferred, pFileInfo) -> {
            ResultFileInfoWrite res = winfsp.write(
                    fs(pFS),
                    pFileContext,
                    pBuffer,
                    offset,
                    length,
                    writeToEndOfFile,
                    constrainedIo
            );

            if (res.getNtStatus() == 0 || res.getNtStatus() == 0x103) {
                pBytesTransferred.putLong(0, res.getBytesTransferred());
                putFileInfo(pFileInfo, res);
            }

            return res.getNtStatus();
        });
    }

    static void initFlush(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Flush.set((pFS, pFileContext, pFileInfo) -> {
            ResultFileInfo res = winfsp.flush(fs(pFS), pFileContext);

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
            }

            return res.getNtStatus();
        });
    }

    static void initGetFileInfo(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.GetFileInfo.set((pFS, pFileContext, pFileInfo) -> {
            ResultFileInfo res = winfsp.getFileInfo(fs(pFS), pFileContext);

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
            }

            return res.getNtStatus();
        });
    }

    static void initSetBasicInfo(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.SetBasicInfo.set((pFS, pFileContext, fileAttributes, creationTime, lastAccessTime, lastWriteTime, changeTime,
                              pFileInfo) -> {
            ResultFileInfo res = winfsp.setBasicInfo(
                    fs(pFS),
                    pFileContext,
                    fileAttributes,
                    creationTime,
                    lastAccessTime,
                    lastWriteTime,
                    changeTime
            );

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
            }

            return res.getNtStatus();
        });
    }

    static void initSetFileSize(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.SetFileSize.set((pFS, pFileContext, newSize, setAllocationSize, pFileInfo) -> {
            ResultFileInfo res = winfsp.setFileSize(fs(pFS), pFileContext, newSize, setAllocationSize);

            if (res.getNtStatus() == 0) {
                putFileInfo(pFileInfo, res);
            }

            return res.getNtStatus();
        });
    }

    static void initCanDelete(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.CanDelete.set((pFS, pFileContext, pFileName) -> {
            Result res = winfsp.canDelete(fs(pFS), pFileContext, getString(pFileName));
            return res.getNtStatus();
        });
    }

    static void initRename(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.Rename.set((pFS, pFileContext, pFileName, pNewFileName, replaceIfExists) -> {
            Result res = winfsp.rename(
                    fs(pFS),
                    pFileContext,
                    getString(pFileName),
                    getString(pNewFileName),
                    replaceIfExists
            );
            return res.getNtStatus();
        });
    }

    static void initGetSecurity(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.GetSecurity.set((pFS, pFileContext, pSecurityDescriptor, pSecurityDescriptorSize) -> {
            ResultSecurity res = winfsp.getSecurity(fs(pFS), pFileContext);

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
            Result res = winfsp.setSecurity(fs(pFS), pFileContext, securityInformation, pModificationDescriptor);
            return res.getNtStatus();
        });
    }

    static void initReadDirectory(FSP_FILE_SYSTEM_INTERFACE fsi, WinFspFS winfsp) {
        fsi.ReadDirectory.set((pFS, pFileContext, pPattern, pMarker, pBuffer, length, pBytesTransferred) -> {
            ResultRead res = winfsp.readDirectory(
                    fs(pFS),
                    pFileContext,
                    getString(pPattern),
                    getString(pMarker),
                    pBuffer,
                    length
            );

            if (res.getNtStatus() == 0 || res.getNtStatus() == 0x103) {
                pBytesTransferred.putLong(0, res.getBytesTransferred());
            }

            return res.getNtStatus();
        });
    }

    private static FSP_FILE_SYSTEM fs(Pointer pFS) {
        return FSP_FILE_SYSTEM.of(pFS).get();
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

    private static void putFileContext(Pointer ppFileContext, ResultFileInfoAndContext res) {
        ppFileContext.putPointer(0, res.getpFileContext());
    }

    /**
     * Reads a null-terminated string using the configured charset for decoding the bytes.
     * <p>
     * This code is adapted from jnr.ffi.provider.converters.StringResultConverter but with a
     * small fix to handle the case where a null terminator character is shorter than the
     * configured terminator length (2 in the case of UTF-16{LE|BE})
     *
     * @param pStr A pointer to a string
     * @return a string (null if the pointer is null)
     */
    private static String getString(Pointer pStr) {
        if (pStr == null) {
            return null;
        }

        Search:
        for (int idx = 0; ; ) {
            idx += pStr.indexOf(idx, (byte) 0);
            for (int tcount = 1; tcount < terminatorLength; tcount++) {
                byte b = pStr.getByte(idx + tcount);
                if (b != 0) {
                    idx += tcount;
                    continue Search;
                }
            }

            // Small fix here to accommodate enough bytes for the string.
            // NOTE: this WILL NOT make the string include a null character
            while (idx % terminatorLength != 0)
                idx++;

            byte[] bytes = new byte[idx];
            pStr.get(0, bytes, 0, bytes.length);
            try {
                return getDecoder().reset().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException cce) {
                throw new RuntimeException(cce);
            }
        }
    }

    private static CharsetDecoder getDecoder() {
        Reference<CharsetDecoder> ref = localDecoder.get();
        CharsetDecoder decoder;
        return ref != null && (decoder = ref.get()) != null && decoder.charset() == CS
                ? decoder : initDecoder();
    }

    private static CharsetDecoder initDecoder() {
        CharsetDecoder decoder = CS.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        localDecoder.set(new SoftReference<>(decoder));

        return decoder;
    }
}
