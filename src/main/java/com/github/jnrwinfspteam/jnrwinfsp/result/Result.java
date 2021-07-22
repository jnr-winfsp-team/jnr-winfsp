package com.github.jnrwinfspteam.jnrwinfsp.result;

public class Result {

    private final int ntStatus;

    public Result() {
        this.ntStatus = 0;
    }

    public Result(int ntStatus) {
        this.ntStatus = ntStatus;
    }

    public final int getNtStatus() {
        return ntStatus;
    }
}
