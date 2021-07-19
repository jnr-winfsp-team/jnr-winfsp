package com.github.jnrwinfspteam.jnrwinfsp.result;

import com.github.jnrwinfspteam.jnrwinfsp.FileContext;
import com.github.jnrwinfspteam.jnrwinfsp.util.Pointered;

public class ResultFileInfoAndContext extends ResultFileInfo {

    private final Pointered<FileContext> fileContextP;

    public ResultFileInfoAndContext(int ntStatus, Pointered<FileContext> fileContextP) {
        super(ntStatus);
        this.fileContextP = fileContextP;
    }

    public final Pointered<FileContext> getFileContextP() {
        return fileContextP;
    }
}
