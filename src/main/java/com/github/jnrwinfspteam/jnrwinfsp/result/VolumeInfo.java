package com.github.jnrwinfspteam.jnrwinfsp.result;

public class VolumeInfo {

    private final long totalSize;
    private final long freeSize;
    private final String volumeLabel;

    public VolumeInfo(long totalSize, long freeSize, String volumeLabel) {
        this.totalSize = totalSize;
        this.freeSize = freeSize;
        this.volumeLabel = volumeLabel;
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
