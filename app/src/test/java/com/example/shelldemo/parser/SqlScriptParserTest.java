package com.example.shelldemo.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

class SqlScriptParserTest {

    @TempDir
    Path tempDir;
    
    private File plsqlScriptFile;
    private File mixedScriptFile;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create a test file with PL/SQL blocks
        plsqlScriptFile = tempDir.resolve("plsql_script.sql").toFile();
        try (FileWriter writer = new FileWriter(plsqlScriptFile)) {
            writer.write("-- Drop the function if it exists\n");
            writer.write("BEGIN\n");
            writer.write("   EXECUTE IMMEDIATE 'DROP FUNCTION hr.get_employee_info';\n");
            writer.write("EXCEPTION\n");
            writer.write("   WHEN OTHERS THEN\n");
            writer.write("      IF SQLCODE != -4043 THEN  -- -4043 is \"does not exist\"\n");
            writer.write("         RAISE;\n");
            writer.write("      END IF;\n");
            writer.write("END;\n");
            writer.write("/\n\n");
            
            writer.write("-- Create the function in HR schema\n");
            writer.write("CREATE OR REPLACE FUNCTION hr.get_employee_info(p_emp_id IN NUMBER)\n"); 
            writer.write("RETURN VARCHAR2 AS\n");
            writer.write("BEGIN\n");
            writer.write("    RETURN (SELECT first_name || ' ' || last_name\n"); 
            writer.write("            FROM hr.employees\n");
            writer.write("            WHERE employee_id = p_emp_id);\n");
            writer.write("EXCEPTION\n");
            writer.write("    WHEN NO_DATA_FOUND THEN\n");
            writer.write("        RETURN NULL;\n");
            writer.write("    WHEN OTHERS THEN\n");
            writer.write("        RAISE;\n");
            writer.write("END;\n");
            writer.write("/\n\n");
            
            writer.write("-- Grant execute permission to public\n");
            writer.write("GRANT EXECUTE ON\n");
            writer.write("hr.get_employee_info TO PUBLIC;\n");
        }
        
        // Create a mixed SQL and PL/SQL file
        mixedScriptFile = tempDir.resolve("mixed_script.sql").toFile();
        try (FileWriter writer = new FileWriter(mixedScriptFile)) {
            writer.write("-- Create a table\n");
            writer.write("CREATE TABLE employees (\n");
            writer.write("    employee_id NUMBER PRIMARY KEY,\n");
            writer.write("    first_name VARCHAR2(50),\n");
            writer.write("    last_name VARCHAR2(50)\n");
            writer.write(");\n\n");
            
            writer.write("-- Insert some data\n");
            writer.write("INSERT INTO employees VALUES (1, 'John', 'Doe');\n\n");
            writer.write("INSERT INTO employees VALUES (2, 'Jane', 'Smith');\n\n");
            
            writer.write("-- Create a simple procedure\n");
            writer.write("CREATE OR REPLACE PROCEDURE get_employee_count AS\n");
            writer.write("    v_count NUMBER;\n");
            writer.write("BEGIN\n");
            writer.write("    SELECT COUNT(*) INTO v_count FROM employees;\n");
            writer.write("    DBMS_OUTPUT.PUT_LINE('Employee count: ' || v_count);\n");
            writer.write("END;\n");
            writer.write("/\n");
        }
    }
    
    @Test
    void testParsePLSQLBlocksWithForwardSlashDelimiter() {
        // Parse the PL/SQL script
        Map<Integer, String> statements = SqlScriptParser.parseSqlFile(plsqlScriptFile);
        
        // Print all statements for debugging
        System.out.println("PL/SQL Script - " + statements.size() + " statements:");
        for (Map.Entry<Integer, String> entry : statements.entrySet()) {
            System.out.println("Statement #" + entry.getKey() + ":");
            System.out.println(entry.getValue());
            System.out.println("---");
        }
        
        // We should have exactly 3 statements with our new approach:
        // 1. The BEGIN...END block (first PL/SQL block)
        // 2. The CREATE FUNCTION...END block (second PL/SQL block)
        // 3. The GRANT statement
        assertEquals(3, statements.size(), "Should parse 3 statements");
        
        // First statement should be the DROP FUNCTION block
        assertTrue(
            statements.get(1).contains("BEGIN") && 
            statements.get(1).contains("EXECUTE IMMEDIATE") && 
            statements.get(1).contains("END"),
            "First statement should be the complete PL/SQL block for dropping function"
        );
        
        // Second statement should be the CREATE FUNCTION block
        assertTrue(
            statements.get(2).contains("CREATE OR REPLACE FUNCTION") && 
            statements.get(2).contains("RETURN VARCHAR2") && 
            statements.get(2).contains("END"),
            "Second statement should be the complete PL/SQL block for creating function"
        );
        
        // Third statement should be the GRANT
        assertTrue(
            statements.get(3).contains("GRANT EXECUTE"),
            "Third statement should be the GRANT statement"
        );
    }
    
    @Test
    void testParseMixedSqlAndPlsql() {
        // Parse the mixed script
        Map<Integer, String> statements = SqlScriptParser.parseSqlFile(mixedScriptFile);
        
        // Print all statements for debugging
        System.out.println("Mixed Script - " + statements.size() + " statements:");
        for (Map.Entry<Integer, String> entry : statements.entrySet()) {
            System.out.println("Statement #" + entry.getKey() + ":");
            System.out.println(entry.getValue());
            System.out.println("---");
        }
        
        // We should have 4 statements with our revised approach:
        // 1. CREATE TABLE
        // 2. First INSERT
        // 3. Second INSERT
        // 4. CREATE PROCEDURE (PL/SQL block)
        assertEquals(4, statements.size(), "Should parse 4 statements");
        
        // CREATE TABLE statement
        assertTrue(
            statements.containsValue("CREATE TABLE employees (\n    employee_id NUMBER PRIMARY KEY,\n    first_name VARCHAR2(50),\n    last_name VARCHAR2(50)\n);") || 
            statements.containsValue("CREATE TABLE employees (\nemployee_id NUMBER PRIMARY KEY,\nfirst_name VARCHAR2(50),\nlast_name VARCHAR2(50)\n);"),
            "Should have a CREATE TABLE statement"
        );
        
        // INSERT statements
        assertTrue(
            statements.containsValue("INSERT INTO employees VALUES (1, 'John', 'Doe');"),
            "Should have an INSERT statement for John Doe"
        );
        assertTrue(
            statements.containsValue("INSERT INTO employees VALUES (2, 'Jane', 'Smith');"),
            "Should have an INSERT statement for Jane Smith"
        );
        
        // PL/SQL block
        boolean hasProcedure = false;
        for (String stmt : statements.values()) {
            if (stmt.contains("CREATE OR REPLACE PROCEDURE") && 
                stmt.contains("BEGIN") && 
                stmt.contains("END")) {
                hasProcedure = true;
                break;
            }
        }
        assertTrue(hasProcedure, "Should have a CREATE PROCEDURE PL/SQL block");
    }
    
    @Test
    void testParseMultiLineComments() throws IOException {
        // Create a file with multi-line comments
        File multiLineCommentFile = tempDir.resolve("multiline_comments.sql").toFile();
        try (FileWriter writer = new FileWriter(multiLineCommentFile)) {
            writer.write("/* This is a multi-line comment\n");
            writer.write("   that spans multiple lines\n");
            writer.write("   and should be ignored */\n");
            writer.write("CREATE TABLE test_table (\n");
            writer.write("    id NUMBER PRIMARY KEY, /* inline comment */\n");
            writer.write("    /* comment before column */ name VARCHAR2(50),\n");
            writer.write("    /* Start of a multi-line comment\n");
            writer.write("       that continues here\n");
            writer.write("       and ends here */ description VARCHAR2(200)\n");
            writer.write(");\n\n");
            
            writer.write("-- Single-line comment\n");
            writer.write("INSERT INTO test_table VALUES (1, 'Test Name' /* comment */, 'Test Description');\n");
        }
        
        // Parse the file with multi-line comments
        Map<Integer, String> statements = SqlScriptParser.parseSqlFile(multiLineCommentFile);
        
        // Print all statements for debugging
        System.out.println("Multi-line Comment Test - " + statements.size() + " statements:");
        for (Map.Entry<Integer, String> entry : statements.entrySet()) {
            System.out.println("Statement #" + entry.getKey() + ":");
            System.out.println(entry.getValue());
            System.out.println("---");
        }
        
        // We should have 2 statements:
        // 1. CREATE TABLE
        // 2. INSERT INTO
        assertEquals(2, statements.size(), "Should parse 2 statements");
        
        // Verify the CREATE TABLE statement (should not contain any comments)
        String createTableStmt = statements.get(1);
        assertFalse(createTableStmt.contains("/*"), "CREATE TABLE statement should not contain comment markers");
        assertFalse(createTableStmt.contains("*/"), "CREATE TABLE statement should not contain comment markers");
        assertTrue(createTableStmt.contains("id NUMBER PRIMARY KEY"), "CREATE TABLE statement should contain column definitions");
        assertTrue(createTableStmt.contains("name VARCHAR2(50)"), "CREATE TABLE statement should contain column definitions");
        assertTrue(createTableStmt.contains("description VARCHAR2(200)"), "CREATE TABLE statement should contain column definitions");
        
        // Verify the INSERT statement (should not contain any comments)
        String insertStmt = statements.get(2);
        assertFalse(insertStmt.contains("/*"), "INSERT statement should not contain comment markers");
        assertFalse(insertStmt.contains("*/"), "INSERT statement should not contain comment markers");
        assertTrue(insertStmt.contains("INSERT INTO test_table VALUES"), "INSERT statement should be properly parsed");
    }
} 