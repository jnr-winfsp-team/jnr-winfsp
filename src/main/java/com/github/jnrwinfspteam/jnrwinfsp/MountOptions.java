package com.github.jnrwinfspteam.jnrwinfsp;

import java.util.Objects;

public class MountOptions {

    boolean debug = false;
    CaseOption caseOption = CaseOption.CASE_SENSITIVE;
    int sectorSize = 4096;
    int sectorsPerAllocationUnit = 1;

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
