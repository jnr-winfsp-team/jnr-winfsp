package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.result.ResultFileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultFileInfoAndContext;
import com.github.jnrwinfspteam.jnrwinfsp.result.ResultVolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import jnr.ffi.Pointer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainTest {
    public static void main(String[] args) throws MountException, IOException {
        Path mountPoint = null;
        if (args.length > 0)
            mountPoint = Paths.get(args[0]);

        var testFS = new TestWinFspFS();
        System.out.printf("Mounting %s...%n", mountPoint == null ? "" : mountPoint);
        testFS.mountLocalDrive(mountPoint, true);
        try {
            System.out.println("<Press Enter to quit>");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } finally {
            System.out.printf("Unmounting %s...%n", mountPoint == null ? "" : mountPoint);
            testFS.unmountLocalDrive();
            System.out.println("<done>");
        }
    }

    public static class TestWinFspFS extends WinFspStubFS {
        @Override
        public ResultVolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) {
            System.out.println("=== GET VOLUME INFO");
            System.out.println(fileSystem);
            try {
                FileStore fStore = Files.getFileStore(Paths.get("C:"));
                return new ResultVolumeInfo(
                        0,
                        fStore.getTotalSpace(),
                        fStore.getUsableSpace(),
                        "TESTDRIVE"

                );
            } catch (Exception e) {
                e.printStackTrace();
                return new ResultVolumeInfo(-1);
            }
        }

        @Override
        public ResultVolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) {
            System.out.println("=== SET VOLUME LABEL " + volumeLabel);
            try {
                FileStore fStore = Files.getFileStore(Paths.get("C:"));
                return new ResultVolumeInfo(
                        0,
                        fStore.getTotalSpace(),
                        fStore.getUsableSpace(),
                        volumeLabel

                );
            } catch (Exception e) {
                e.printStackTrace();
                return new ResultVolumeInfo(-1);
            }
        }

        @Override
        public ResultFileInfoAndContext create(FSP_FILE_SYSTEM fileSystem,
                                               String fileName,
                                               int createOptions,
                                               int grantedAccess,
                                               int fileAttributes,
                                               Pointer pSecurityDescriptor,
                                               long allocationSize) {

            System.out.println("=== CREATE " + fileName);
            return new ResultFileInfoAndContext(0, Pointer.newIntPointer(jnr.ffi.Runtime.getSystemRuntime(), 0));
        }

        @Override
        public ResultFileInfoAndContext open(FSP_FILE_SYSTEM fileSystem,
                                             String fileName,
                                             int createOptions,
                                             int grantedAccess) {

            System.out.println("=== OPEN " + fileName);
            return new ResultFileInfoAndContext(0, Pointer.newIntPointer(jnr.ffi.Runtime.getSystemRuntime(), 0));
        }

        @Override
        public ResultFileInfo overwrite(FSP_FILE_SYSTEM fileSystem,
                                        Pointer pFileContext,
                                        int fileAttributes,
                                        boolean replaceFileAttributes,
                                        long allocationSize) {

            System.out.println("=== OVERWRITE");
            return new ResultFileInfo(0);
        }
    }
}
