package com.github.jnrwinfspteam.jnrwinfsp.result;

public class ResultFileInfo extends Result {

    private int fileAttributes;
    private int reparseTag;
    private long allocationSize;
    private long fileSize;
    private long creationTime;
    private long lastAccessTime;
    private long lastWriteTime;
    private long changeTime;
    private long indexNumber;
    private final int hardLinks = 0; /* unimplemented: set to 0 */
    private int eaSize;

    public ResultFileInfo(int ntStatus) {
        super(ntStatus);
    }

    public final int getFileAttributes() {
        return fileAttributes;
    }

    public final void setFileAttributes(int fileAttributes) {
        this.fileAttributes = fileAttributes;
    }

    public final int getReparseTag() {
        return reparseTag;
    }

    public final void setReparseTag(int reparseTag) {
        this.reparseTag = reparseTag;
    }

    public final long getAllocationSize() {
        return allocationSize;
    }

    public final void setAllocationSize(long allocationSize) {
        this.allocationSize = allocationSize;
    }

    public final long getFileSize() {
        return fileSize;
    }

    public final void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public final long getCreationTime() {
        return creationTime;
    }

    public final void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public final long getLastAccessTime() {
        return lastAccessTime;
    }

    public final void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public final long getLastWriteTime() {
        return lastWriteTime;
    }

    public final void setLastWriteTime(long lastWriteTime) {
        this.lastWriteTime = lastWriteTime;
    }

    public final long getChangeTime() {
        return changeTime;
    }

    public final void setChangeTime(long changeTime) {
        this.changeTime = changeTime;
    }

    public final long getIndexNumber() {
        return indexNumber;
    }

    public final void setIndexNumber(long indexNumber) {
        this.indexNumber = indexNumber;
    }

    public final int getHardLinks() {
        return hardLinks;
    }

    public final int getEaSize() {
        return eaSize;
    }

    public final void setEaSize(int eaSize) {
        this.eaSize = eaSize;
    }
}
