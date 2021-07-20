package com.github.jnrwinfspteam.jnrwinfsp.result;

import jnr.ffi.Pointer;

public class ResultFileInfoAndContext extends ResultFileInfo {

    private final Pointer pFileContext;

    public ResultFileInfoAndContext(int ntStatus, Pointer pFileContext) {
        super(ntStatus);
        this.pFileContext = pFileContext;
    }

    public final Pointer getpFileContext() {
        return pFileContext;
    }
}
