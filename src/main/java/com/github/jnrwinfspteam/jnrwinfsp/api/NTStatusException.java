package com.github.jnrwinfspteam.jnrwinfsp.api;

public class NTStatusException extends Exception {

    // TODO add exception constants for common NT statuses

    private final int ntStatus;

    public NTStatusException(int ntStatus) {
        this.ntStatus = ntStatus;
    }

    public int getNtStatus() {
        return ntStatus;
    }
}
