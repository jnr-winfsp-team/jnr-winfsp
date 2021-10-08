package com.github.jnrwinfspteam.jnrwinfsp.api;

public class MountException extends Exception {

    private int ntStatus;

    public MountException(String message) {
        super(message);
    }

    public MountException(String message, Throwable cause) {
        super(message, cause);
    }

    public MountException(String message, int ntStatus) {
        this(String.format("%s: returned NT STATUS of %08X", message, ntStatus));
        this.ntStatus = ntStatus;
    }

    public int getNtStatus() {
        return ntStatus;
    }
}
