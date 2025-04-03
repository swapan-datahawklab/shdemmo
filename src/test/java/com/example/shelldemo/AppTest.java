package com.example.shelldemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.MDC;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for App class.
 */
class AppTest {
    private App app;
    private CommandLine cmd;

    @BeforeEach
    void setUp() {
        app = new App();
        cmd = new CommandLine(app);
        MDC.put("applicationName", "TestApp");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testDefaultGreeting() {
        assertEquals(0, cmd.execute());
    }

    @Test
    void testCustomNameGreeting() {
        assertEquals(0, cmd.execute("--name", "Alice"));
    }

    @Test
    void testVerboseOutput() {
        assertEquals(0, cmd.execute("--verbose"));
    }

    @Test
    void testVersionOption() {
        assertEquals(0, cmd.execute("--version"));
    }

    @Test
    void testHelpOption() {
        assertEquals(0, cmd.execute("--help"));
    }
}
