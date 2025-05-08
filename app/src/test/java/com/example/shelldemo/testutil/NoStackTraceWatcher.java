package com.example.shelldemo.testutil;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;

/**
 * JUnit 5 extension that provides custom test execution reporting without stack traces.
 * Integrates with Log4j2 for consistent logging across the application.
 */
public class NoStackTraceWatcher implements TestWatcher {
    private static final Logger logger = LogManager.getLogger(NoStackTraceWatcher.class);
    private static final String SEPARATOR = "----------------------------------------";

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        logTestStatus(context, "DISABLED", reason.orElse("No reason provided"));
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        logTestStatus(context, "PASSED", null);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        logTestStatus(context, "ABORTED", cause.getMessage());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testName = getTestName(context);
        String className = getClassName(context);
        
        // Extract just the essential error message
        String errorMsg = cause.getMessage();
        if (errorMsg != null) {
            // Extract only the error code and actual error message
            String[] lines = errorMsg.split("\n");
            if (lines.length > 0) {
                StringBuilder cleanedMsg = new StringBuilder();
                for (String line : lines) {
                    line = line.trim();
                    if (line.contains("Command failed with exit code")) {
                        cleanedMsg.append(line).append("\n");
                    } else if (line.startsWith("Output:")) {
                        String output = line.substring(7).trim();
                        int usageIndex = output.indexOf("Usage:");
                        if (usageIndex > 0) {
                            output = output.substring(0, usageIndex).trim();
                        }
                        cleanedMsg.append(output);
                        break;
                    }
                }
                errorMsg = cleanedMsg.toString().trim();
            }
        }
        
        // Log a single, concise error message
        logger.error("Test '{}' in {} failed: {}", testName, className, errorMsg);
    }

    private void logTestStatus(ExtensionContext context, String status, String message) {
        String testName = getTestName(context);
        String className = getClassName(context);
        
        StringBuilder logMessage = new StringBuilder()
            .append("\n").append(SEPARATOR)
            .append("\nTest ").append(status).append(": ").append(testName)
            .append("\nClass: ").append(className);

        if (message != null) {
            logMessage.append("\nDetails: ").append(message);
        }

        logMessage.append("\n").append(SEPARATOR);

        if ("PASSED".equals(status)) {
            logger.info(logMessage.toString());
        } else {
            logger.warn(logMessage.toString());
        }
    }

    private String getTestName(ExtensionContext context) {
        return context.getTestMethod()
                     .map(method -> {
                         DisplayName displayName = method.getAnnotation(DisplayName.class);
                         return displayName != null ? displayName.value() : method.getName();
                     })
                     .orElse("Unknown Test");
    }

    private String getClassName(ExtensionContext context) {
        return context.getTestClass()
                     .map(Class::getSimpleName)
                     .orElse("Unknown Class");
    }
}
