package com.github.jnrwinfspteam.jnrwinfsp.service;

import com.github.jnrwinfspteam.jnrwinfsp.api.MountException;
import com.github.jnrwinfspteam.jnrwinfsp.api.MountOptions;
import com.github.jnrwinfspteam.jnrwinfsp.api.Mountable;
import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import com.github.jnrwinfspteam.jnrwinfsp.internal.util.StringUtils;
import jnr.ffi.Pointer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class exposes the WinFsp Service functionality via JNR.
 * <p>
 * Processes calling these methods can run as Windows services started with e.g., {@code sc.exe} or the
 * WinFsp.Launcher.
 */
public final class ServiceRunner {

    public static void runAsService(String serviceName, OnStart onStart, OnStop onStop) throws ServiceException {
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(onStart);
        Objects.requireNonNull(onStop);

        int ntStatus = LibWinFsp.INSTANCE.FspServiceRunEx(
                StringUtils.toBytes(serviceName, true),
                (pService, argc, argv) -> {
                    Pointer[] pStrs = new Pointer[argc];
                    argv.get(0, pStrs, 0, pStrs.length);
                    String[] args = Arrays.stream(pStrs)
                            .map(StringUtils::fromPointer)
                            .toArray(String[]::new);

                    return onStart.onStart(args);
                },
                (pService) -> onStop.onStop(),
                null,
                null
        );

        if (ntStatus != 0)
            throw new ServiceException(ntStatus);
    }

    public static void mountLocalDriveAsService(String serviceName,
                                                Mountable mountable,
                                                Path mountPoint) throws ServiceException {
        mountLocalDriveAsService(serviceName, mountable, mountPoint, new MountOptions());
    }

    public static void mountLocalDriveAsService(String serviceName,
                                                Mountable mountable,
                                                Path mountPoint,
                                                MountOptions options) throws ServiceException {
        Objects.requireNonNull(mountable);
        Objects.requireNonNull(options);

        runAsService(
                serviceName,
                (args) -> {
                    try {
                        mountable.mountLocalDrive(mountPoint, options);
                        return 0;
                    } catch (MountException e) {
                        return e.getNtStatus();
                    }
                },
                () -> {
                    mountable.unmountLocalDrive();
                    return 0;
                }
        );
    }
}
