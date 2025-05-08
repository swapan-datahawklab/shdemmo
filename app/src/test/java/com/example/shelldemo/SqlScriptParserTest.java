package com.example.shelldemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.parser.SqlScriptParser;

@DisplayName("SQL Script Parser Tests")
class SqlScriptParserTest {

    @TempDir
    Path tempDir;
    
    private File plsqlScriptFile;
    private File mixedScriptFile;
    
    @BeforeEach
    void setUp() throws IOException {
        createPlSqlTestFile();
        createMixedSqlTestFile();
    }
    
    private void createPlSqlTestFile() throws IOException {
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
    }
    
    private void createMixedSqlTestFile() throws IOException {
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

    @Nested
    @DisplayName("PL/SQL Block Tests")
    class PlSqlBlockTests {
        @Test
        @DisplayName("Should parse PL/SQL blocks with forward slash delimiter")
        void testParsePLSQLBlocksWithForwardSlashDelimiter() {
            Map<Integer, String> statements = SqlScriptParser.parseSqlFile(plsqlScriptFile);
            
            assertEquals(3, statements.size(), "Should parse 3 statements");
            
            // First statement should be the DROP FUNCTION block
            String dropBlock = statements.get(1);
            assertTrue(dropBlock.contains("BEGIN") && 
                      dropBlock.contains("EXECUTE IMMEDIATE") && 
                      dropBlock.contains("END"),
                "First statement should be the complete PL/SQL block for dropping function");
            
            // Second statement should be the CREATE FUNCTION block
            String createBlock = statements.get(2);
            assertTrue(createBlock.contains("CREATE OR REPLACE FUNCTION") && 
                      createBlock.contains("RETURN VARCHAR2") && 
                      createBlock.contains("END"),
                "Second statement should be the complete PL/SQL block for creating function");
            
            // Third statement should be the GRANT
            assertTrue(statements.get(3).contains("GRANT EXECUTE"),
                "Third statement should be the GRANT statement");
        }
        
        @Test
        @DisplayName("Should handle nested PL/SQL blocks")
        void testNestedPlSqlBlocks() throws IOException {
            File nestedBlocksFile = tempDir.resolve("nested_blocks.sql").toFile();
            try (FileWriter writer = new FileWriter(nestedBlocksFile)) {
                writer.write("CREATE OR REPLACE PROCEDURE nested_test AS\n");
                writer.write("BEGIN\n");
                writer.write("    BEGIN\n");
                writer.write("        NULL;\n");
                writer.write("    END;\n");
                writer.write("END;\n");
                writer.write("/\n");
            }
            
            Map<Integer, String> statements = SqlScriptParser.parseSqlFile(nestedBlocksFile);
            assertEquals(1, statements.size(), "Should parse nested blocks as one statement");
            assertTrue(statements.get(1).contains("BEGIN") && 
                      statements.get(1).contains("END") && 
                      statements.get(1).contains("NULL"),
                "Should preserve nested block structure");
        }
    }

    @Nested
    @DisplayName("Mixed SQL and PL/SQL Tests")
    class MixedSqlTests {
        @Test
        @DisplayName("Should parse mixed SQL and PL/SQL statements")
        void testParseMixedSqlAndPlsql() {
            Map<Integer, String> statements = SqlScriptParser.parseSqlFile(mixedScriptFile);
            
            assertEquals(4, statements.size(), "Should parse 4 statements");
            
            // Verify CREATE TABLE statement
            assertTrue(statements.values().stream()
                .anyMatch(stmt -> stmt.startsWith("CREATE TABLE employees")),
                "Should have a CREATE TABLE statement");
            
            // Verify INSERT statements
            assertTrue(statements.values().stream()
                .anyMatch(stmt -> stmt.equals("INSERT INTO employees VALUES (1, 'John', 'Doe');")),
                "Should have first INSERT statement");
            assertTrue(statements.values().stream()
                .anyMatch(stmt -> stmt.equals("INSERT INTO employees VALUES (2, 'Jane', 'Smith');")),
                "Should have second INSERT statement");
            
            // Verify PL/SQL procedure
            assertTrue(statements.values().stream()
                .anyMatch(stmt -> stmt.contains("CREATE OR REPLACE PROCEDURE") && 
                                stmt.contains("BEGIN") && 
                                stmt.contains("END")),
                "Should have CREATE PROCEDURE block");
        }
    }

    @Nested
    @DisplayName("Comment Handling Tests")
    class CommentTests {
        @Test
        @DisplayName("Should handle multi-line comments")
        void testParseMultiLineComments() throws IOException {
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
            
            Map<Integer, String> statements = SqlScriptParser.parseSqlFile(multiLineCommentFile);
            
            assertEquals(2, statements.size(), "Should parse 2 statements");
            
            // Verify CREATE TABLE statement
            String createTableStmt = statements.get(1);
            assertFalse(createTableStmt.contains("/*"), "Should not contain comment markers");
            assertFalse(createTableStmt.contains("*/"), "Should not contain comment markers");
            assertTrue(createTableStmt.contains("id NUMBER PRIMARY KEY"), 
                "Should preserve column definitions");
            assertTrue(createTableStmt.contains("name VARCHAR2(50)"), 
                "Should preserve column definitions");
            assertTrue(createTableStmt.contains("description VARCHAR2(200)"), 
                "Should preserve column definitions");
            
            // Verify INSERT statement
            String insertStmt = statements.get(2);
            assertFalse(insertStmt.contains("/*"), "Should not contain comment markers");
            assertFalse(insertStmt.contains("*/"), "Should not contain comment markers");
            assertTrue(insertStmt.contains("INSERT INTO test_table VALUES"), 
                "Should preserve INSERT statement");
        }
        
        @Test
        @DisplayName("Should handle nested comments")
        void testNestedComments() throws IOException {
            File nestedCommentsFile = tempDir.resolve("nested_comments.sql").toFile();
            try (FileWriter writer = new FileWriter(nestedCommentsFile)) {
                writer.write("/* Outer comment /* Inner comment */ still outer */ SELECT 1 FROM dual;");
            }
            
            Map<Integer, String> statements = SqlScriptParser.parseSqlFile(nestedCommentsFile);
            assertEquals(1, statements.size(), "Should parse one statement");
            assertEquals("SELECT 1 FROM dual;", statements.get(1).trim(), 
                "Should remove all nested comments");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        @Test
        @DisplayName("Should throw exception for null file")
        void testNullFile() {
            assertThrows(DatabaseException.class, 
                () -> SqlScriptParser.parseSqlFile(null),
                "Should throw DatabaseException for null file");
        }
        
        @Test
        @DisplayName("Should throw exception for non-existent file")
        void testNonExistentFile() {
            File nonExistentFile = tempDir.resolve("non_existent.sql").toFile();
            assertThrows(DatabaseException.class, 
                () -> SqlScriptParser.parseSqlFile(nonExistentFile),
                "Should throw DatabaseException for non-existent file");
        }
        
        @Test
        @DisplayName("Should handle unclosed comments")
        void testUnclosedComments() throws IOException {
            File unclosedCommentFile = tempDir.resolve("unclosed_comment.sql").toFile();
            try (FileWriter writer = new FileWriter(unclosedCommentFile)) {
                writer.write("/* This comment is not closed\n");
                writer.write("SELECT * FROM table;\n");
            }
            
            Map<Integer, String> statements = SqlScriptParser.parseSqlFile(unclosedCommentFile);
            assertTrue(statements.isEmpty(), "Should handle unclosed comments gracefully");
        }
    }
} 