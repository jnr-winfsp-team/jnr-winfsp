package com.github.jnrwinfspteam.jnrwinfsp.service;

import com.github.jnrwinfspteam.jnrwinfsp.internal.lib.LibWinFsp;
import jnr.ffi.Pointer;

import java.time.Duration;
import java.util.Objects;

public final class ServiceControl {

    private final Pointer pService;

    ServiceControl(Pointer pService) {
        this.pService = Objects.requireNonNull(pService);
    }

    public void requestAdditionalTime(Duration time) {
        LibWinFsp.INSTANCE.FspServiceRequestTime(pService, Math.toIntExact(time.toMillis()));
    }
}
