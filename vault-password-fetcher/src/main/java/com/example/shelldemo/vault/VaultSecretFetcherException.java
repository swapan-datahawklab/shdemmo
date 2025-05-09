package com.example.shelldemo.vault;

public class VaultSecretFetcherException extends Exception {
    public VaultSecretFetcherException(String message) {
        super(message);
    }
    public VaultSecretFetcherException(String message, Throwable cause) {
        super(message, cause);
    }
} 