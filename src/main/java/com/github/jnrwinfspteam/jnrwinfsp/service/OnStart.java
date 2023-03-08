package com.github.jnrwinfspteam.jnrwinfsp.service;

@FunctionalInterface
public interface OnStart {

    /**
     * Called when the service is started (e.g., when the WinFsp.Launcher runs with an instruction for starting a
     * service instance).
     *
     * @return STATUS_SUCCESS or error status
     */
    int onStart(String[] args, ServiceControl control);
}
