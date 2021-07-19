package com.github.jnrwinfspteam.jnrwinfsp.result;

import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_FILE_INFO;

public class ResultFileInfoWrite extends ResultFileInfo {

    private final long bytesTransferred;

    public ResultFileInfoWrite(int ntStatus, FSP_FSCTL_FILE_INFO fileInfo, long bytesTransferred) {
        super(ntStatus, fileInfo);
        this.bytesTransferred = bytesTransferred;
    }

    public final long getBytesTransferred() {
        return bytesTransferred;
    }
}
