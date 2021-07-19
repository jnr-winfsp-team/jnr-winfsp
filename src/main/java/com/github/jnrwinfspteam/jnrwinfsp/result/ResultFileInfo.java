package com.github.jnrwinfspteam.jnrwinfsp.result;

import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_FILE_INFO;

public class ResultFileInfo extends Result {

    private final FSP_FSCTL_FILE_INFO fileInfo;

    public ResultFileInfo(int ntStatus, FSP_FSCTL_FILE_INFO fileInfo) {
        super(ntStatus);
        this.fileInfo = fileInfo;
    }

    public final FSP_FSCTL_FILE_INFO getFileInfo() {
        return fileInfo;
    }
}
