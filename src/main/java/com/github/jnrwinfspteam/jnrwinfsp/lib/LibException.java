package com.github.jnrwinfspteam.jnrwinfsp.lib;

public class LibException extends RuntimeException {

    public LibException(String message) {
        super(message);
    }

    public LibException(String message, Throwable cause) {
        super(message, cause);
    }
}
