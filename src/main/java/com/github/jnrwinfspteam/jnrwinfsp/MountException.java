package com.github.jnrwinfspteam.jnrwinfsp;

public class MountException extends Exception {

    public MountException(String message) {
        super(message);
    }

    public MountException(String message, Throwable cause) {
        super(message, cause);
    }

    public MountException(String message, int ntStatus) {
        this(String.format("%s: returned NT STATUS of %X", message, ntStatus));
    }
}
