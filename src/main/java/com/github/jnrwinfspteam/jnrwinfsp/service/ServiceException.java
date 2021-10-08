package com.github.jnrwinfspteam.jnrwinfsp.service;

public class ServiceException extends Exception {

    private final int ntStatus;

    public ServiceException(int ntStatus) {
        super(String.format("service run returned NT STATUS of %08X", ntStatus));
        this.ntStatus = ntStatus;
    }

    public int getNtStatus() {
        return ntStatus;
    }
}
