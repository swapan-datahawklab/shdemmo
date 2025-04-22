package com.example.integration;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PicocliTest {

    @Test
    void testPicocliCommand() {
        String[] args = {"--db-type", "oracle"};
        int exitCode = new CommandLine(new PicocliTestCommand()).execute(args);
        assertEquals(0, exitCode, "Command should execute successfully");
    }

    @Test
    void testMissingRequiredOption() {
        String[] args = {};
        int exitCode = new CommandLine(new PicocliTestCommand()).execute(args);
        assertEquals(2, exitCode, "Command should fail when required option is missing");
    }
} 