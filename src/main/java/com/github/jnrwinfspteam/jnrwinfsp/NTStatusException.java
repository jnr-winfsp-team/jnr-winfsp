package com.github.jnrwinfspteam.jnrwinfsp;

public class NTStatusException extends Exception {

    private final int ntStatus;

    public NTStatusException(int ntStatus) {
        this.ntStatus = ntStatus;
    }

    public int getNtStatus() {
        return ntStatus;
    }
}
