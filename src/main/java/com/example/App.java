package com.example;

import com.example.oracle.cli.OracleRunnerCli;

public class App {
    public static void main(String[] args) {
        try {
            run(args);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) {
        if (args.length < 1) {
            printUsage();
            throw new RuntimeException("No command specified");
        }

        String command = args[0];
        String[] remainingArgs = new String[args.length - 1];
        System.arraycopy(args, 1, remainingArgs, 0, args.length - 1);

        switch (command) {
            case "script":
                runSqlScript(remainingArgs);
                break;
            case "proc":
                runStoredProc(remainingArgs);
                break;
            default:
                printUsage();
                throw new RuntimeException("Unknown command: " + command);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  script -H <host> -u <username> -p <password> <script-file> [options]");
        System.out.println("    Options:");
        System.out.println("      --stop-on-error=<true|false>   Stop on error (default: true)");
        System.out.println("      --auto-commit=<true|false>     Auto-commit mode (default: false)");
        System.out.println("      --print-statements=<true|false> Print statements (default: false)");
        System.out.println();
        System.out.println("  proc -H <host> -u <username> -p <password> <proc-name> [options]");
        System.out.println("    Options:");
        System.out.println("      --function                     Execute as function");
        System.out.println("      --return-type=<type>          Return type for functions (default: NUMERIC)");
        System.out.println("      -i <name:type:value,...>      Input parameters");
        System.out.println("      -o <name:type,...>            Output parameters");
        System.out.println("      --io <name:type:value,...>    Input/Output parameters");
    }

    private static void runSqlScript(String[] args) {
        OracleRunnerCli.main(args);
    }

    private static void runStoredProc(String[] args) {
        OracleRunnerCli.main(args);
    }
}
