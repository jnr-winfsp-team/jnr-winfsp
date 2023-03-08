package com.github.jnrwinfspteam.jnrwinfsp.service;

@FunctionalInterface
public interface OnStop {

    /**
     * Called when the service is stopped (e.g., when the WinFsp.Launcher runs with an instruction for stopping a
     * service instance).
     *
     * @return STATUS_SUCCESS or error status
     */
    int onStop(ServiceControl control);
}
