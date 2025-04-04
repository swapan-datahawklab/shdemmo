package com.example.oracle;

import com.example.oracle.cli.OracleRunnerCli;

/**
 * Main entry point for the Oracle database utilities.
 * Provides access to both the CLI runner and example implementations.
 */
public class App {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("example")) {
            // Remove "example" from args before passing to StoredProcExample
            String[] exampleArgs = new String[args.length - 1];
            System.arraycopy(args, 1, exampleArgs, 0, args.length - 1);
            StoredProcExample.main(exampleArgs);
        } else {
            // Default to CLI runner
            OracleRunnerCli.main(args);
        }
    }
} 