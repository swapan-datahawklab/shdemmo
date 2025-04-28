package com.example.shelldemo.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// import java.util.List;
// import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

import com.example.shelldemo.exception.SqlParseException;

/**
 * Parses SQL script files into individual SQL statements.
 * Handles:
 * - Single-line comments (--)
 * - Multi-line comments (/* ... *\/)
 * - Statement delimiters (;)
 * - Quoted strings (to avoid parsing comments or delimiters within strings)
 */
public final class SqlScriptParser {
    private static final Logger logger = LogManager.getLogger(SqlScriptParser.class);
    // private static final String SEMICOLON_DELIMITER = ";";
    // private static final String FORWARD_SLASH_DELIMITER = "/";
    // private static final String SINGLE_LINE_COMMENT = "--";
    // private static final String MULTI_LINE_COMMENT_START = "/*";
    // private static final String MULTI_LINE_COMMENT_END = "*/";
    // private static final Pattern WHITESPACE = Pattern.compile("^\\s*$");
    private SqlScriptParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    
    // private static final Pattern PLSQL_START = Pattern.compile("^(BEGIN|DECLARE|CREATE\\s+(OR\\s+REPLACE\\s+)?(" +
    //     "FUNCTION|PROCEDURE|TRIGGER|PACKAGE|PACKAGE\\s+BODY|" +
    //     "TYPE|TYPE\\s+BODY|VIEW|MATERIALIZED\\s+VIEW|" +
    //     "LIBRARY|JAVA|CONTEXT|DIRECTORY|SYNONYM|EDITION|" +
    //     "DATABASE\\s+TRIGGER|INSTEAD\\s+OF\\s+TRIGGER))\\s+.*", 
    //     Pattern.CASE_INSENSITIVE);



    /**
     * Parses a SQL script file into a map of individual SQL statements.
     *
     * @param scriptFile the SQL script file to parse
     * @return Map of statement numbers to SQL statements
     * @throws SqlParseException if parsing fails
     */
    public static Map<Integer, String> parseSqlFile(File scriptFile) throws SqlParseException {
        try {
            SqlParseException.validateScriptFile(scriptFile);
            
            Map<Integer, String> statements = new HashMap<>();
            StringBuilder currentStatement = new StringBuilder();
            boolean inPlsqlBlock = false;
            int statementCount = 0;
            int plsqlLevel = 0; // Track nested BEGIN/END blocks
            boolean inMultiLineComment = false;

            // Read all lines into a single string to help with multi-line processing
            String content = new String(Files.readAllBytes(scriptFile.toPath()));
            
            // First, preprocess the content to remove comments
            StringBuilder processedContent = new StringBuilder();
            boolean inSingleQuote = false;
            boolean inDoubleQuote = false;
            boolean inLineComment = false;
            inMultiLineComment = false;
            
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                char next = (i < content.length() - 1) ? content.charAt(i + 1) : '\0';
                
                // Reset line comment flag at the end of line
                if ((c == '\n' || c == '\r') && inLineComment) {
                    inLineComment = false;
                    processedContent.append(c); // Keep the newlines
                    continue;
                }
                
                // Skip everything in comments
                if (inLineComment || inMultiLineComment) {
                    // Check for end of multi-line comment
                    if (inMultiLineComment && c == '*' && next == '/') {
                        inMultiLineComment = false;
                        i++; // Skip the /
                    }
                    continue;
                }
                
                // Handle string literals (don't interpret comments inside strings)
                if (c == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                    processedContent.append(c);
                    continue;
                } else if (c == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                    processedContent.append(c);
                    continue;
                }
                
                // Only process comments if not inside a string
                if (!inSingleQuote && !inDoubleQuote) {
                    // Check for start of line comment
                    if (c == '-' && next == '-') {
                        inLineComment = true;
                        i++; // Skip the next -
                        continue;
                    }
                    
                    // Check for start of multi-line comment
                    if (c == '/' && next == '*') {
                        inMultiLineComment = true;
                        i++; // Skip the *
                        continue;
                    }
                }
                
                processedContent.append(c);
            }
            
            // Now process the content without comments
            String[] lines = processedContent.toString().replaceAll("\r\n", "\n").split("\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }
                
                // Handle forward slash delimiter for PL/SQL blocks
                if (line.equals("/")) {
                    if (inPlsqlBlock) {
                        // This is a PL/SQL block terminator
                        statements.put(++statementCount, currentStatement.toString().trim());
                        currentStatement.setLength(0);
                        inPlsqlBlock = false;
                        plsqlLevel = 0;
                    } else if (currentStatement.length() > 0) {
                        // Stray slash - just add it to the current statement
                        currentStatement.append(line).append("\n");
                    }
                    continue;
                }
                
                // Check for PL/SQL block start indicators
                if (!inPlsqlBlock && 
                    (line.toUpperCase().startsWith("BEGIN") || 
                     line.toUpperCase().startsWith("DECLARE") ||
                     (line.toUpperCase().startsWith("CREATE") && 
                      (line.toUpperCase().contains(" FUNCTION") || 
                       line.toUpperCase().contains(" PROCEDURE") || 
                       line.toUpperCase().contains(" TRIGGER") || 
                       line.toUpperCase().contains(" PACKAGE"))))) {
                    
                    inPlsqlBlock = true;
                    plsqlLevel = 1; // Starting a new block
                    
                    // If we have accumulated content, start a new statement
                    if (currentStatement.length() > 0) {
                        statements.put(++statementCount, currentStatement.toString().trim());
                        currentStatement.setLength(0);
                    }
                    
                    currentStatement.append(line).append("\n");
                    continue;
                }
                
                // Handle regular SQL statements or continue with PL/SQL
                if (inPlsqlBlock) {
                    // Track nested BEGIN/END blocks to handle complex PL/SQL
                    if (line.toUpperCase().contains("BEGIN")) {
                        plsqlLevel++;
                    } else if (line.toUpperCase().contains("END;")) {
                        plsqlLevel--;
                        
                        // Check if this is the end of the entire PL/SQL block
                        if (plsqlLevel <= 0) {
                            currentStatement.append(line).append("\n");
                            statements.put(++statementCount, currentStatement.toString().trim());
                            currentStatement.setLength(0);
                            inPlsqlBlock = false;
                            plsqlLevel = 0;
                            continue;
                        }
                    }
                    
                    // Continue accumulating the PL/SQL block
                    currentStatement.append(line).append("\n");
                } else {
                    // Handle regular SQL statements
                    currentStatement.append(line);
                    
                    // Check for statement termination with semicolon
                    if (line.endsWith(";")) {
                        statements.put(++statementCount, currentStatement.toString().trim());
                        currentStatement.setLength(0);
                    } else {
                        // Add a newline if not terminated with semicolon
                        currentStatement.append("\n");
                    }
                }
            }
            
            // Add any remaining statement that wasn't terminated
            if (currentStatement.length() > 0) {
                String remainingStmt = currentStatement.toString().trim();
                if (!remainingStmt.isEmpty()) {
                    statements.put(++statementCount, remainingStmt);
                }
            }
            
            logger.info("Successfully parsed {} SQL statements from file", statements.size());
            return statements;
        } catch (IOException e) {
            throw new SqlParseException(
                "Failed to parse SQL file: " + scriptFile.getName(), 
                e, 
                SqlParseException.ERROR_CODE_FILE_IO_ERROR
            );
        } catch (IllegalArgumentException e) {
            throw new SqlParseException(
                e.getMessage(), 
                e, 
                SqlParseException.ERROR_CODE_INVALID_FORMAT
            );
        }
    }

}