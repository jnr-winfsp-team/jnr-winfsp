package com.github.jnrwinfspteam.jnrwinfsp.result;

import com.github.jnrwinfspteam.jnrwinfsp.FileContext;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FSCTL_FILE_INFO;

public class ResultFileInfoAndContext extends ResultFileInfo {

    private final FileContext fileContext;

    public ResultFileInfoAndContext(int ntStatus, FSP_FSCTL_FILE_INFO fileInfo, FileContext fileContext) {
        super(ntStatus, fileInfo);
        this.fileContext = fileContext;
    }

    public final FileContext getFileContext() {
        return fileContext;
    }
}
