package com.example.integration;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "test-picocli", mixinStandardHelpOptions = true, version = "1.0")
public class PicocliTestCommand implements Runnable {

    @Option(names = "--db-type", required = true)
    String dbType;

    @Override
    public void run() {
        System.out.println("DB Type: " + dbType);
    }

    public static void main(String[] args) {
        new CommandLine(new PicocliTestCommand()).execute(args);
    }
} 