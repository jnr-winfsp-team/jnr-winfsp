package com.github.jnrwinfspteam.jnrwinfsp.result;

public class ResultVolumeInfo extends Result {

    private final long totalSize;
    private final long freeSize;
    private final String volumeLabel;

    public ResultVolumeInfo(int ntStatus, long totalSize, long freeSize, String volumeLabel) {
        super(ntStatus);
        this.totalSize = totalSize;
        this.freeSize = freeSize;
        this.volumeLabel = volumeLabel;
    }

    public ResultVolumeInfo(int ntStatus) {
        this(ntStatus, 0, 0, "");
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getFreeSize() {
        return freeSize;
    }

    public String getVolumeLabel() {
        return volumeLabel;
    }
}
