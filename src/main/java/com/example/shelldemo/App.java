package com.example.shelldemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Main application class demonstrating Picocli command-line interface.
 */
@Command(
    name = "shelldemo",
    version = "1.0",
    description = "A demo shell application using Picocli",
    mixinStandardHelpOptions = true  // Adds --help and --version options
)
public class App implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Option(
        names = {"-n", "--name"},
        description = "Name to greet",
        defaultValue = "World"
    )
    private String name;

    @Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose output"
    )
    private boolean verbose;

    public static void main(String[] args) {
        try {
            MDC.put("applicationName", "ShellDemo");
            logger.debug("Initializing application with args: {}", String.join(" ", args));
            logger.info("Starting application");
            
            int exitCode = new CommandLine(new App()).execute(args);
            
            logger.debug("Application execution completed");
            logger.info("Application completed with exit code: {}", exitCode);
            System.exit(exitCode);
        } catch (Exception e) {
            logger.error("Application failed to start", e);
            System.exit(1);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Helper method to log at QUIET level
     */
    private void logQuiet(String message) {
        System.out.print(message);
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Processing command with name='{}', verbose={}", name, verbose);
            
            if (verbose) {
                logger.debug("Verbose mode enabled");
            }
            
            String greeting = "Hello, " + name + "!";
            logger.debug("Generated greeting: {}", greeting);
            logQuiet(greeting);
            
            return 0; // Success exit code
        } catch (Exception e) {
            logger.error("Error during application execution", e);
            return 1; // Error exit code
        }
    }
}
