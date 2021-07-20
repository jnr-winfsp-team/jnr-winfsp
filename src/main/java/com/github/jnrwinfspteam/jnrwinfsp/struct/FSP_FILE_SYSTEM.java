package com.github.jnrwinfspteam.jnrwinfsp.struct;

import com.github.jnrwinfspteam.jnrwinfsp.lib.FSP;
import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;
import jnr.ffi.NativeType;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FSP_FILE_SYSTEM extends Struct {

    public final Struct.Unsigned16 Version = new Unsigned16();
    public final Struct.Pointer UserContext = new Pointer();      /* PVOID */
    public final Struct.Padding VolumeName
            = new Padding(NativeType.USHORT, FSP.FSCTL_VOLUME_NAME_SIZEMAX);
    public final Struct.Pointer VolumeHandle = new Pointer();     /* HANDLE */
    public final Struct.Pointer EnterOperation = new Pointer();   /* FSP_FILE_SYSTEM_OPERATION_GUARD */
    public final Struct.Pointer LeaveOperation = new Pointer();   /* FSP_FILE_SYSTEM_OPERATION_GUARD */
    public final Struct.Pointer[] Operations
            = array(new Pointer[FSP.FsctlTransactKindCount]);     /* FSP_FILE_SYSTEM_OPERATION[] */
    public final Struct.Pointer Interface = new Pointer();        /* FSP_FILE_SYSTEM_INTERFACE */
    public final Struct.Pointer DispatcherThread = new Pointer(); /* HANDLE */
    public final Struct.Unsigned32 DispatcherThreadCount = new Unsigned32();
    public final Struct.Unsigned32 DispatcherResult = new Unsigned32();
    public final Struct.Pointer MountPoint = new Pointer();       /* PWSTR */
    public final Struct.Pointer MountHandle = new Pointer();      /* HANDLE */
    public final Struct.Unsigned32 DebugLog = new Struct.Unsigned32();
    public final Struct.Enum<OPERATION_GUARD_STRATEGY> OpGuardStrategy
            = new Enum<>(OPERATION_GUARD_STRATEGY.class);
    public final Struct.Pointer OpGuardLock = new Pointer();      /* SRWLOCK */
    public final Struct.Boolean UmFileContextIsUserContext2 = new Boolean();
    public final Struct.Boolean UmFileContextIsFullContext = new Boolean();

    public static Pointered<FSP_FILE_SYSTEM> of(jnr.ffi.Pointer pointer) {
        return Pointered.wrap(new FSP_FILE_SYSTEM(Runtime.getSystemRuntime()), pointer);
    }

    private FSP_FILE_SYSTEM(Runtime runtime) {
        super(runtime);
    }

    /**
     * User mode file system locking strategy.
     * <p>
     * Two concurrency models are provided:
     * <p>
     * 1. A fine-grained concurrency model where file system NAMESPACE accesses
     * are guarded using an exclusive-shared (read-write) lock. File I/O is not
     * guarded and concurrent reads/writes/etc. are possible. [Note that the FSD
     * will still apply an exclusive-shared lock PER INDIVIDUAL FILE, but it will
     * not limit I/O operations for different files.]
     * <p>
     * The fine-grained concurrency model applies the exclusive-shared lock as
     * follows:
     * <ul>
     * <li>EXCL: SetVolumeLabel, Flush(Volume),
     * Create, Cleanup(Delete), SetInformation(Rename)</li>
     * <li>SHRD: GetVolumeInfo, Open, SetInformation(Disposition), ReadDirectory</li>
     * <li>NONE: all other operations</li>
     * </ul>
     * <p>
     * 2. A coarse-grained concurrency model where all file system accesses are
     * guarded by a mutually exclusive lock.
     */
    public enum OPERATION_GUARD_STRATEGY {
        FINE,
        COARSE;
    }
}
