package com.github.jnrwinfspteam.jnrwinfsp.internal.struct;

import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.WinFspCallbacks;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.Pointered;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FSP_FILE_SYSTEM_INTERFACE extends Struct {

    public final Struct.Function<WinFspCallbacks.GetVolumeInfoCallback> GetVolumeInfo =
            function(WinFspCallbacks.GetVolumeInfoCallback.class);

    public final Struct.Function<WinFspCallbacks.SetVolumeLabelCallback> SetVolumeLabel =
            function(WinFspCallbacks.SetVolumeLabelCallback.class);

    public final Struct.Function<WinFspCallbacks.GetSecurityByNameCallback> GetSecurityByName =
            function(WinFspCallbacks.GetSecurityByNameCallback.class);

    {
        // NOTE: this ensures that the interface struct is correctly defined.
        // A stand-in for Create, which is not supported; instead we support CreateEx.
        for (int i = 0; i < 1; i++) {
            // actual function is irrelevant here, we just need the function pointers
            function(WinFspCallbacks.GetSecurityByNameCallback.class);
        }
    }

    public final Struct.Function<WinFspCallbacks.OpenCallback> Open =
            function(WinFspCallbacks.OpenCallback.class);

    public final Struct.Function<WinFspCallbacks.OverwriteCallback> Overwrite =
            function(WinFspCallbacks.OverwriteCallback.class);

    public final Struct.Function<WinFspCallbacks.CleanupCallback> Cleanup =
            function(WinFspCallbacks.CleanupCallback.class);

    public final Struct.Function<WinFspCallbacks.CloseCallback> Close =
            function(WinFspCallbacks.CloseCallback.class);

    public final Struct.Function<WinFspCallbacks.ReadCallback> Read =
            function(WinFspCallbacks.ReadCallback.class);

    public final Struct.Function<WinFspCallbacks.WriteCallback> Write =
            function(WinFspCallbacks.WriteCallback.class);

    public final Struct.Function<WinFspCallbacks.FlushCallback> Flush =
            function(WinFspCallbacks.FlushCallback.class);

    public final Struct.Function<WinFspCallbacks.GetFileInfoCallback> GetFileInfo =
            function(WinFspCallbacks.GetFileInfoCallback.class);

    public final Struct.Function<WinFspCallbacks.SetBasicInfoCallback> SetBasicInfo =
            function(WinFspCallbacks.SetBasicInfoCallback.class);

    public final Struct.Function<WinFspCallbacks.SetFileSizeCallback> SetFileSize =
            function(WinFspCallbacks.SetFileSizeCallback.class);

    public final Struct.Function<WinFspCallbacks.CanDeleteCallback> CanDelete =
            function(WinFspCallbacks.CanDeleteCallback.class);

    public final Struct.Function<WinFspCallbacks.RenameCallback> Rename =
            function(WinFspCallbacks.RenameCallback.class);

    public final Struct.Function<WinFspCallbacks.GetSecurityCallback> GetSecurity =
            function(WinFspCallbacks.GetSecurityCallback.class);

    public final Struct.Function<WinFspCallbacks.SetSecurityCallback> SetSecurity =
            function(WinFspCallbacks.SetSecurityCallback.class);

    public final Struct.Function<WinFspCallbacks.ReadDirectoryCallback> ReadDirectory =
            function(WinFspCallbacks.ReadDirectoryCallback.class);

    public final Struct.Function<WinFspCallbacks.ResolveReparsePointsCallback> ResolveReparsePoints =
            function(WinFspCallbacks.ResolveReparsePointsCallback.class);

    public final Struct.Function<WinFspCallbacks.GetReparsePointCallback> GetReparsePoint =
            function(WinFspCallbacks.GetReparsePointCallback.class);

    public final Struct.Function<WinFspCallbacks.SetReparsePointCallback> SetReparsePoint =
            function(WinFspCallbacks.SetReparsePointCallback.class);

    public final Struct.Function<WinFspCallbacks.DeleteReparsePointCallback> DeleteReparsePoint =
            function(WinFspCallbacks.DeleteReparsePointCallback.class);

    {
        // NOTE: this ensures that the interface struct is correctly defined.
        // A stand-in for 1 unsupported operation.
        for (int i = 0; i < 1; i++) {
            // actual function is irrelevant here, we just need the function pointers
            function(WinFspCallbacks.DeleteReparsePointCallback.class);
        }
    }

    public final Struct.Function<WinFspCallbacks.GetDirInfoByNameCallback> GetDirInfoByName =
            function(WinFspCallbacks.GetDirInfoByNameCallback.class);

    {
        // NOTE: this ensures that the interface struct is correctly defined.
        // A stand-in for 2 unsupported operations.
        for (int i = 0; i < 2; i++) {
            // actual function is irrelevant here, we just need the function pointers
            function(WinFspCallbacks.GetDirInfoByNameCallback.class);
        }
    }

    public final Struct.Function<WinFspCallbacks.CreateExCallback> CreateEx =
            function(WinFspCallbacks.CreateExCallback.class);

    {
        // NOTE: this ensures that the interface struct is correctly defined.
        // Starting index must be equal to number of functions defined above.
        // Ending index must be 63 (struct has 64 entries).
        for (int i = 28; i < 64; i++) {
            // actual function is irrelevant here, we just need the function pointers
            function(WinFspCallbacks.CreateExCallback.class);
        }
    }

    public static Pointered<FSP_FILE_SYSTEM_INTERFACE> create(Runtime runtime) {
        var fspi = new FSP_FILE_SYSTEM_INTERFACE(runtime);

        // allocate the necessary memory for the struct
        return Pointered.allocate(fspi);
    }

    private FSP_FILE_SYSTEM_INTERFACE(Runtime runtime) {
        super(runtime);
    }
}