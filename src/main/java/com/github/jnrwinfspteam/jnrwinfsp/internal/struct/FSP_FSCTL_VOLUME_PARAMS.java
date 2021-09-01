package com.github.jnrwinfspteam.jnrwinfsp.internal.struct;

import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.FSP;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.Pointered;
import jnr.ffi.NativeType;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public final class FSP_FSCTL_VOLUME_PARAMS extends Struct {
    // FSP_FSCTL_VOLUME_PARAMS_V0_FIELD_DEFN
    public final Struct.Unsigned16 Version = new Unsigned16();              /* set to 0 or sizeof(FSP_FSCTL_VOLUME_PARAMS) */
    /* volume information */
    public final Struct.Unsigned16 SectorSize = new Unsigned16();
    public final Struct.Unsigned16 SectorsPerAllocationUnit = new Unsigned16();
    public final Struct.Unsigned16 MaxComponentLength = new Unsigned16();   /* maximum file name component length (bytes) */
    public final Struct.Unsigned64 VolumeCreationTime = new Unsigned64();
    public final Struct.Unsigned32 VolumeSerialNumber = new Unsigned32();
    /* I/O timeouts, capacity, etc. */
    public final Struct.Unsigned32 TransactTimeout = new Unsigned32();      /* DEPRECATED: (millis;  1 sec - 10 sec) */
    public final Struct.Unsigned32 IrpTimeout = new Unsigned32();           /* pending IRP timeout (millis;  1 min - 10 min) */
    public final Struct.Unsigned32 IrpCapacity = new Unsigned32();          /* maximum number of pending IRP's (100 - 1000)*/
    public final Struct.Unsigned32 FileInfoTimeout = new Unsigned32();      /* FileInfo/Security/VolumeInfo timeout (millis) */
    /* FILE_FS_ATTRIBUTE_INFORMATION::FileSystemAttributes */
    public final Struct.Unsigned32 FileSystemAttributes = new Unsigned32();
    public final Struct.Padding Prefix
            = new Padding(NativeType.USHORT, FSP.FSCTL_VOLUME_PREFIX_SIZE); /* UNC prefix (\Server\Share) */
    public final Struct.Padding FileSystemName
            = new Padding(NativeType.USHORT, FSP.FSCTL_VOLUME_FSNAME_SIZE);

    // FSP_FSCTL_VOLUME_PARAMS_V1_FIELD_DEFN\
    /* additional fields; specify .Version == sizeof(FSP_FSCTL_VOLUME_PARAMS) */
    public final Unsigned32 ValidFlags = new Unsigned32(); // FIXME access individual flags
    // UINT32 VolumeInfoTimeoutValid:1;    /* VolumeInfoTimeout field is valid */
    // UINT32 DirInfoTimeoutValid:1;       /* DirInfoTimeout field is valid */
    // UINT32 SecurityTimeoutValid:1;      /* SecurityTimeout field is valid*/
    // UINT32 StreamInfoTimeoutValid:1;    /* StreamInfoTimeout field is valid */
    // UINT32 EaTimeoutValid:1;            /* EaTimeout field is valid */
    // UINT32 KmAdditionalReservedFlags:27;
    public final Struct.Unsigned32 VolumeInfoTimeout = new Unsigned32();    /* volume info timeout (millis); overrides FileInfoTimeout */
    public final Struct.Unsigned32 DirInfoTimeout = new Unsigned32();       /* dir info timeout (millis); overrides FileInfoTimeout */
    public final Struct.Unsigned32 SecurityTimeout = new Unsigned32();      /* security info timeout (millis); overrides FileInfoTimeout */
    public final Struct.Unsigned32 StreamInfoTimeout = new Unsigned32();    /* stream info timeout (millis); overrides FileInfoTimeout */
    public final Struct.Unsigned32 EaTimeout = new Unsigned32();            /* EA timeout (millis); overrides FileInfoTimeout */
    public final Struct.Unsigned32 FsextControlCode = new Unsigned32();
    public final Struct.Unsigned32[] Reserved32 = array(new Unsigned32[1]);
    public final Struct.Unsigned64[] Reserved64 = array(new Unsigned64[2]);

    public static Pointered<FSP_FSCTL_VOLUME_PARAMS> create(Runtime runtime) {
        var vp = new FSP_FSCTL_VOLUME_PARAMS(runtime);

        // allocate the necessary memory for the struct
        Pointered<FSP_FSCTL_VOLUME_PARAMS> vpP = Pointered.allocate(vp);

        // initialise every member to zero
        vp.Version.set(0);
        vp.SectorSize.set(0);
        vp.SectorsPerAllocationUnit.set(0);
        vp.MaxComponentLength.set(0);
        vp.VolumeCreationTime.set(0);
        vp.VolumeSerialNumber.set(0);
        vp.TransactTimeout.set(0);
        vp.IrpTimeout.set(0);
        vp.IrpCapacity.set(0);
        vp.FileInfoTimeout.set(0);
        vp.FileSystemAttributes.set(0);
        vp.ValidFlags.set(0);
        vp.VolumeInfoTimeout.set(0);
        vp.DirInfoTimeout.set(0);
        vp.SecurityTimeout.set(0);
        vp.StreamInfoTimeout.set(0);
        vp.EaTimeout.set(0);
        vp.FsextControlCode.set(0);
        vp.Reserved32[0].set(0);
        vp.Reserved64[0].set(0);
        vp.Reserved64[1].set(0);

        return vpP;
    }

    private FSP_FSCTL_VOLUME_PARAMS(Runtime runtime) {
        super(runtime);
    }

    public void setFileSystemAttribute(FSAttr attribute, boolean bitValue) {
        long value = this.FileSystemAttributes.get();

        if (bitValue)
            value = attribute.setBit(value);
        else
            value = attribute.clearBit(value);

        this.FileSystemAttributes.set(value);
    }

    public static enum FSAttr {
        CaseSensitiveSearch(0),       /* file system supports case-sensitive file names */
        CasePreservedNames(1),        /* file system preserves the case of file names */
        UnicodeOnDisk(2),             /* file system supports Unicode in file names */
        PersistentAcls(3),            /* file system preserves and enforces access control lists */
        ReparsePoints(4),             /* file system supports reparse points */
        ReparsePointsAccessCheck(5),  /* file system performs reparse point access checks */
        NamedStreams(6),              /* file system supports named streams */
        HardLinks(7),                 /* unimplemented; set to 0 */
        ExtendedAttributes(8),        /* file system supports extended attributes */
        ReadOnlyVolume(9),

        //         /* kernel-mode flags */
        PostCleanupWhenModifiedOnly(10),   /* post Cleanup when a file was modified/deleted */
        PassQueryDirectoryPattern(11),     /* pass Pattern during QueryDirectory operations */
        AlwaysUseDoubleBuffering(12),
        PassQueryDirectoryFileName(13),    /* pass FileName during QueryDirectory (GetDirInfoByName) */
        FlushAndPurgeOnCleanup(14),        /* keeps file off "standby" list */
        DeviceControl(15),                 /* support user-mode ioctl handling */

        //         /* user-mode flags */
        UmFileContextIsUserContext2(16),   /* user mode: FileContext parameter is UserContext2 */
        UmFileContextIsFullContext(17),    /* user mode: FileContext parameter is FullContext */

        // UmReservedFlags:6,

        //         /* additional kernel-mode flags */
        AllowOpenInKernelMode(24),         /* allow kernel mode to open files when possible */
        CasePreservedExtendedAttributes(25),   /* preserve case of EA (default is UPPERCASE) */
        WslFeatures(26),                   /* support features required for WSLinux */
        DirectoryMarkerAsNextOffset(27),   /* directory marker is next offset instead of last name */
        RejectIrpPriorToTransact0(28);     /* reject IRP's prior to FspFsctlTransact with 0 buffers */

        // KmReservedFlags:3;

        private final int bitShift;

        private FSAttr(int bitShift) {
            this.bitShift = bitShift;
        }

        long setBit(long value) {
            return value | (1L << this.bitShift);
        }

        long clearBit(long value) {
            return value & (~(1L << this.bitShift));
        }
    }
}