package com.github.jnrwinfspteam.jnrwinfsp.api;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Predicate;

public class MountOptions {

    private boolean debug = false;
    private PrintStream errorPrinter = null;
    private Predicate<Throwable> errorFilter = null;
    private CaseOption caseOption = CaseOption.CASE_SENSITIVE;
    private int sectorSize = 4096;
    private int sectorsPerAllocationUnit = 1;
    private long fileInfoTimeout = 1000;
    private boolean wslFeatures = true;
    private int maxFileNameLength = 255;
    private boolean forceBuiltinAdminOwnerAndGroup = false;

    /**
     * Sets "debug" option (default is {@code false}).
     *
     * @param debug If true, then debugging output will be printed to the standard error stream
     */
    public MountOptions setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * Sets "errorPrinter" option (default is {@code null}.
     *
     * @param errorPrinter If non-null, then jnr-winfsp errors will be printed to the provided print stream
     */
    public MountOptions setErrorPrinter(PrintStream errorPrinter) {
        this.errorPrinter = errorPrinter;
        return this;
    }

    /**
     * Sets "errorFilter" option (default is {@code null}.
     *
     * @param errorFilter If non-null, then only jnr-winfsp errors which pass the filter will be printed to a print
     *                    stream (if any is configured via {@link #setDebug(boolean)} or
     *                    {@link #setErrorPrinter(PrintStream)}).
     */
    public MountOptions setErrorFilter(Predicate<Throwable> errorFilter) {
        this.errorFilter = errorFilter;
        return this;
    }

    /**
     * Sets "file system case" option (default is {@link CaseOption#CASE_SENSITIVE}).
     *
     * @param caseOption File system configuration according to the case of filenames
     */
    public MountOptions setCase(CaseOption caseOption) {
        this.caseOption = Objects.requireNonNull(caseOption);
        return this;
    }

    /**
     * Sets "sector size" option (default is 4096).
     *
     * @param sectorSize File system fragment size
     */
    public MountOptions setSectorSize(int sectorSize) {
        this.sectorSize = sectorSize;
        return this;
    }

    /**
     * Sets "sectors per allocation unit" option (default is 1).
     *
     * @param sectorsPerAllocationUnit File system fragments per allocation unit
     */
    public MountOptions setSectorsPerAllocationUnit(int sectorsPerAllocationUnit) {
        this.sectorsPerAllocationUnit = sectorsPerAllocationUnit;
        return this;
    }

    /**
     * Sets "file-info timeout" option (default is 1000).
     *
     * @param fileInfoTimeout FileInfo/Security/VolumeInfo timeout (in milliseconds)
     */
    public MountOptions setFileInfoTimeout(long fileInfoTimeout) {
        this.fileInfoTimeout = fileInfoTimeout;
        return this;
    }

    /**
     * Sets "WSL feature" option (default is {@code true}).
     *
     * @param wslFeatures If true, then WSLinux features will be enabled
     */
    public MountOptions setWslFeatures(boolean wslFeatures) {
        this.wslFeatures = wslFeatures;
        return this;
    }

    /**
     * Sets "max file name length" option (default is 255).
     *
     * @param maxFileNameLength Maximum file/folder name length
     */
    public MountOptions setMaxFileNameLength(int maxFileNameLength) {
        this.maxFileNameLength = maxFileNameLength;
        return this;
    }

    /**
     * Sets "Force Built-in Administrator Owner/Group" option (default is {@code false}).
     *
     * @param enable If true, then on Create, the Owner and Group of the security descriptor
     *               will be set to the well known SID for the "Built-in Administrators"
     *               (O:BA G:BA)
     */
    public MountOptions setForceBuiltinAdminOwnerAndGroup(boolean enable) {
        this.forceBuiltinAdminOwnerAndGroup = enable;
        return this;
    }

    public boolean hasDebug() {
        return debug;
    }

    public PrintStream getErrorPrinter() {
        return errorPrinter;
    }

    public Predicate<Throwable> getErrorFilter() {
        return errorFilter;
    }

    public CaseOption getCaseOption() {
        return caseOption;
    }

    public int getSectorSize() {
        return sectorSize;
    }

    public int getSectorsPerAllocationUnit() {
        return sectorsPerAllocationUnit;
    }

    public long getFileInfoTimeout() {
        return fileInfoTimeout;
    }

    public boolean hasWslFeatures() {
        return wslFeatures;
    }

    public int getMaxFileNameLength() {
        return maxFileNameLength;
    }

    public boolean hasForceBuiltinAdminOwnerAndGroup() {
        return forceBuiltinAdminOwnerAndGroup;
    }

    /**
     * Configures a file system according to the case of filenames.
     */
    public enum CaseOption {

        /**
         * File system is case-sensitive (common in Linux file systems).
         * <ul>
         *     <li>Two files/directories with the same name must differ in case.</li>
         *     <li>New files/directories must have their case preserved.</li>
         *     <li>Name search must match exact case.</li>
         * </ul>
         */
        CASE_SENSITIVE,

        /**
         * File system is case-preserving (common in Windows file systems).
         * <ul>
         *     <li>Two files/directories with the same name are not allowed (regardless of case).</li>
         *     <li>New files/directories must have their case preserved.</li>
         *     <li>Name search must ignore case.</li>
         * </ul>
         */
        CASE_PRESERVING,

        /**
         * File system is case-insensitive (uncommon).
         * <ul>
         *     <li>Two files/directories with the same name are not allowed (regardless of case).</li>
         *     <li>New files/directories must NOT have their case preserved (they must have a normalised case).</li>
         *     <li>Name search must ignore case.</li>
         * </ul>
         */
        CASE_INSENSITIVE;
    }
}
