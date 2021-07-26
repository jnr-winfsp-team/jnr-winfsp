package com.github.jnrwinfspteam.jnrwinfsp.lib;

import jnr.ffi.Pointer;

public interface LibKernel32 {

    /* The values to use for Get/SetStdHandle */
    int STD_INPUT_HANDLE = (-10);
    int STD_OUTPUT_HANDLE = (-11);
    int STD_ERROR_HANDLE = (-12);

    /**
     * @param nStdHandle The standard device identifier
     * @return A handle to the specified standard device (standard input, output, or error)
     */
    Pointer GetStdHandle(int nStdHandle);

    Pointer LocalFree(Pointer /* HLOCAL */ hMem);
}