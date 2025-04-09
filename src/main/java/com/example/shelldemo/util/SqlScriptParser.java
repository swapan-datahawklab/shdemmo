package com.example.shelldemo.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


/**
 * Parses a SQL script file into a list of individual SQL statements.
 * Handles single-line (--), multi-line (/* ... * /), and inline comments (--).
 * Statements are expected to be terminated by a semicolon (;).
 */
public class SqlScriptParser {

    private static class ParseState {
        private final StringBuilder currentStatement = new StringBuilder();
        private boolean inMultilineComment = false;
    }

    private SqlScriptParser() {
        // Private constructor to prevent instantiation
    }

    public static List<String> parse(File scriptFile) throws IOException {
        List<String> statements = new ArrayList<>();
        ParseState state = new ParseState();

        for (String line : Files.readAllLines(scriptFile.toPath())) {
            String effectiveLine = processLineContent(line, state.inMultilineComment);
            state.inMultilineComment = isInMultilineComment(line, state.inMultilineComment);

            if (effectiveLine != null) {
                state.currentStatement.append(effectiveLine);
                if (effectiveLine.trim().endsWith(";")) {
                    statements.add(state.currentStatement.toString().trim());
                    state.currentStatement.setLength(0);
                }
            }
        }

        // Add any remaining statement
        if (state.currentStatement.length() > 0) {
            statements.add(state.currentStatement.toString().trim());
        }

        return statements;
    }

    private static String processLineContent(String line, boolean inMultilineComment) {
        if (inMultilineComment) {
            return handleMultilineComment(line);
        }
        return handleRegularLine(line);
    }

    private static String handleMultilineComment(String line) {
        int endCommentIndex = line.indexOf("*/");
        if (endCommentIndex != -1) {
            return line.substring(endCommentIndex + 2).trim();
        }
        return null;
    }

    private static String handleRegularLine(String line) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
            return null;
        }

        int startCommentIndex = trimmedLine.indexOf("/*");
        if (startCommentIndex != -1) {
            return trimmedLine.substring(0, startCommentIndex).trim();
        }

        return trimmedLine;
    }

    private static boolean isInMultilineComment(String line, boolean currentState) {
        if (currentState) {
            return line.indexOf("*/") == -1;
        }
        return line.indexOf("/*") != -1 && line.indexOf("*/") == -1;
    }
}