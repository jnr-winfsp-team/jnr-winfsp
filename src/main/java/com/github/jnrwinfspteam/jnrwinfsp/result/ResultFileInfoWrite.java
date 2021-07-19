package com.github.jnrwinfspteam.jnrwinfsp.result;

public class ResultFileInfoWrite extends ResultFileInfo {

    private final long bytesTransferred;

    public ResultFileInfoWrite(int ntStatus, long bytesTransferred) {
        super(ntStatus);
        this.bytesTransferred = bytesTransferred;
    }

    public final long getBytesTransferred() {
        return bytesTransferred;
    }
}
