package com.github.jnrwinfspteam.jnrwinfsp;

import java.nio.file.Path;

public interface Mountable {

    /**
     * Mounts a local WinFsp drive on the given mount point. If the mount point is null, then
     * it will be the next available drive letter counting downwards from Z:
     *
     * @param mountPoint   A file path (may be null)
     * @param fsCaseOption File system configuration according to the case of filenames
     * @param debug        If true, then debugging output will be printed to the standard error stream
     * @throws MountException       If some problem occurs during mounting, or if the drive is already mounted
     * @throws NullPointerException If {@code fsCaseOption} is null
     */
    void mountLocalDrive(Path mountPoint, FSCaseOption fsCaseOption, boolean debug) throws MountException;

    default void mountLocalDrive(Path mountPoint, FSCaseOption fsCaseOption) throws MountException {
        mountLocalDrive(mountPoint, fsCaseOption, false);
    }

    /**
     * Unmounts the currently mounted WinFsp drive. Will not have any effect if there isn't any currently
     * mounted drive.
     */
    void unmountLocalDrive();

    /**
     * Configures a file system according to the case of filenames.
     */
    enum FSCaseOption {

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
