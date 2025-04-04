package com.example.shelldemo;

import com.example.oracle.cli.OracleRunnerCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;

@SpringBootApplication
@ShellComponent
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @ShellMethod(key = "run-sql", value = "Execute an Oracle SQL script")
    public String runSqlScript(
        @ShellOption(help = "Database host (e.g., localhost:1521/ORCL)") String host,
        @ShellOption(help = "Database username") String username,
        @ShellOption(help = "Database password") String password,
        @ShellOption(help = "SQL script file path") File scriptFile,
        @ShellOption(help = "Stop on error", defaultValue = "true") boolean stopOnError,
        @ShellOption(help = "Auto-commit", defaultValue = "false") boolean autoCommit,
        @ShellOption(help = "Print statements", defaultValue = "false") boolean printStatements
    ) {
        String[] args = new String[] {
            "script",
            "-H", host,
            "-u", username,
            "-p", password,
            "--stop-on-error=" + stopOnError,
            "--auto-commit=" + autoCommit,
            "--print-statements=" + printStatements,
            scriptFile.getAbsolutePath()
        };
        OracleRunnerCli.main(args);
        return "SQL script execution completed";
    }

    @ShellMethod(key = "run-proc", value = "Execute an Oracle stored procedure")
    public String runStoredProc(
        @ShellOption(help = "Database host (e.g., localhost:1521/ORCL)") String host,
        @ShellOption(help = "Database username") String username,
        @ShellOption(help = "Database password") String password,
        @ShellOption(help = "Procedure/function name") String procName,
        @ShellOption(help = "Execute as function", defaultValue = "false") boolean isFunction,
        @ShellOption(help = "Return type for functions (e.g., NUMERIC, VARCHAR)", defaultValue = "NUMERIC") String returnType,
        @ShellOption(help = "Input parameters (name:type:value)", defaultValue = "") String[] inParams,
        @ShellOption(help = "Output parameters (name:type)", defaultValue = "") String[] outParams,
        @ShellOption(help = "Input/Output parameters (name:type:value)", defaultValue = "") String[] inOutParams
    ) {
        String[] args = new String[] {
            "proc",
            "-H", host,
            "-u", username,
            "-p", password,
            procName
        };

        if (isFunction) {
            args = addArg(args, "--function");
            args = addArg(args, "--return-type", returnType);
        }

        if (inParams.length > 0) {
            args = addArg(args, "-i", String.join(",", inParams));
        }

        if (outParams.length > 0) {
            args = addArg(args, "-o", String.join(",", outParams));
        }

        if (inOutParams.length > 0) {
            args = addArg(args, "--io", String.join(",", inOutParams));
        }

        OracleRunnerCli.main(args);
        return "Stored procedure execution completed";
    }

    private String[] addArg(String[] args, String option) {
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = option;
        return newArgs;
    }

    private String[] addArg(String[] args, String option, String value) {
        String[] newArgs = new String[args.length + 2];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = option;
        newArgs[args.length + 1] = value;
        return newArgs;
    }
}
