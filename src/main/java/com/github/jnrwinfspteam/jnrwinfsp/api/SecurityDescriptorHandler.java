package com.github.jnrwinfspteam.jnrwinfsp.api;

public interface SecurityDescriptorHandler {

    /**
     *
     * @param sd
     * @return
     */
    byte[] securityDescriptorToBytes(String sd) throws NTStatusException;

    /**
     *
     * @param sd
     * @return
     */
    String securityDescriptorToString(byte[] sd) throws NTStatusException;
}
