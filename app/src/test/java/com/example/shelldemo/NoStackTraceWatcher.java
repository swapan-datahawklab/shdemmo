package com.example.shelldemo;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class NoStackTraceWatcher implements TestWatcher {
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        System.err.println("Test failed: " + cause.getMessage());
        // Do NOT print the stack trace
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        // Optionally, do nothing or print success
    }
}
