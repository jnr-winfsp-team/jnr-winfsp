package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.flags.CreateOptions;
import com.github.jnrwinfspteam.jnrwinfsp.flags.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.result.*;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;
import jnr.ffi.Pointer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

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
        public VolumeInfo getVolumeInfo(FSP_FILE_SYSTEM fileSystem) throws NTStatusException {
            System.out.println("=== GET VOLUME INFO");
            System.out.println(fileSystem);
            try {
                FileStore fStore = Files.getFileStore(Paths.get("C:"));
                return new VolumeInfo(
                        fStore.getTotalSpace(),
                        fStore.getUsableSpace(),
                        "TESTDRIVE"

                );
            } catch (Exception e) {
                e.printStackTrace();
                throw new NTStatusException(-1);
            }
        }

        @Override
        public VolumeInfo setVolumeLabel(FSP_FILE_SYSTEM fileSystem, String volumeLabel) throws NTStatusException {
            System.out.println("=== SET VOLUME LABEL " + volumeLabel);
            try {
                FileStore fStore = Files.getFileStore(Paths.get("C:"));
                return new VolumeInfo(
                        fStore.getTotalSpace(),
                        fStore.getUsableSpace(),
                        volumeLabel

                );
            } catch (Exception e) {
                e.printStackTrace();
                throw new NTStatusException(-1);
            }
        }

        @Override
        public ResultSecurityAndAttributes getSecurityByName(FSP_FILE_SYSTEM fileSystem, String fileName) {
            System.out.println("=== GET SECURITY BY NAME " + fileName);
            return new ResultSecurityAndAttributes(0, null, 0, 0);
        }

        @Override
        public FileInfo create(FSP_FILE_SYSTEM fileSystem,
                               String fileName,
                               Set<CreateOptions> createOptions,
                               int grantedAccess,
                               Set<FileAttributes> fileAttributes,
                               Pointer pSecurityDescriptor,
                               long allocationSize) {

            System.out.println("=== CREATE " + fileName);
            return new FileInfo(fileName);
        }

        @Override
        public FileInfo open(FSP_FILE_SYSTEM fileSystem,
                             String fileName,
                             Set<CreateOptions> createOptions,
                             int grantedAccess) {

            System.out.println("=== OPEN " + fileName);
            return new FileInfo(fileName);
        }

        @Override
        public FileInfo overwrite(FSP_FILE_SYSTEM fileSystem,
                                  String fileName,
                                  Set<FileAttributes> fileAttributes,
                                  boolean replaceFileAttributes,
                                  long allocationSize) {

            System.out.println("=== OVERWRITE " + fileName);
            return new FileInfo(fileName);
        }

        @Override
        public void close(FSP_FILE_SYSTEM fileSystem, String fileName) {
            System.out.println("=== CLOSE " + fileName);
        }

        @Override
        public long read(FSP_FILE_SYSTEM fileSystem,
                         String fileName,
                         Pointer pBuffer,
                         long offset,
                         long length) {
            System.out.println("=== READ " + fileName);
            return length;
        }

        @Override
        public List<FileInfo> readDirectory(FSP_FILE_SYSTEM fileSystem,
                                            String fileName,
                                            String pattern,
                                            String marker) {
            System.out.println("=== READ DIRECTORY " + fileName);
            return List.of();
        }
    }
}
