package com.github.jnrwinfspteam.jnrwinfsp;

import com.github.jnrwinfspteam.jnrwinfsp.result.ResultVolumeInfo;
import com.github.jnrwinfspteam.jnrwinfsp.struct.FSP_FILE_SYSTEM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainTest {
    public static void main(String[] args) throws MountException, IOException {
        var testFS = new TestWinFspFS();
        System.out.println("Mounting...");
        testFS.mountLocalDrive(null, true);
        try {
            System.out.println("<Press Enter to quit>");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } finally {
            System.out.println("Unmounting...");
            testFS.unmountLocalDrive();
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
    }
}
