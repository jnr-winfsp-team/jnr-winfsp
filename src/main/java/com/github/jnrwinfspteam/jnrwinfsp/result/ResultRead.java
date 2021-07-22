package com.github.jnrwinfspteam.jnrwinfsp.result;

public class ResultRead extends Result {

    private final long bytesTransferred;

    public ResultRead(long bytesTransferred) {
        this(0, bytesTransferred);
    }

    public ResultRead(int ntStatus, long bytesTransferred) {
        super(ntStatus);
        this.bytesTransferred = bytesTransferred;
    }

    public final long getBytesTransferred() {
        return bytesTransferred;
    }
}
