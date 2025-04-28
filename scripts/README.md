# Scripts Directory Documentation

This directory contains various utility scripts for project management, testing, analysis, and documentation. Each script is organized by its primary function.

## Directory Structure

```bash
scripts/
├── analysis/              # Code and project analysis scripts
│   ├── analyze_codebase.sh
│   ├── analyze_structure.sh
│   └── test_source_structure.sh
├── documentation/         # Documentation generation scripts
│   └── document_structure.sh
├── templates/            # Template processing scripts
│   └── process_template.sh
├── testing/             # Test execution scripts
│   ├── test_manager.sh
│   ├── test_memory_bank.sh
│   └── test_unified_db_runner.sh
└── validation/          # Output validation scripts
    └── validate_output.sh
```

## Analysis Scripts

### analyze_codebase.sh

Comprehensive codebase analysis tool.

```bash
./scripts/analysis/analyze_codebase.sh <command> [args...]

Commands:
  structure                     # Show project directory structure
  similar <pattern> [file-pat]  # Find similar code patterns
  packages                      # Analyze Java package structure
  suggest <file-name>          # Suggest location for new file
  check <file-name> [pattern]  # Check for potential duplicates

Examples:
  ./scripts/analysis/analyze_codebase.sh structure
  ./scripts/analysis/analyze_codebase.sh similar 'class.*Controller' '*.java'
  ./scripts/analysis/analyze_codebase.sh packages
  ./scripts/analysis/analyze_codebase.sh suggest NewService.java
  ./scripts/analysis/analyze_codebase.sh check UserService.java 'interface.*Service'
```

### analyze_structure.sh

Analyzes and generates Mermaid diagrams of project structure.

```bash
./scripts/analysis/analyze_structure.sh [options]

Options:
  --exclude-dirs=<dirs>   # Comma-separated list of directories to exclude
  --output=<file>        # Output file for the diagram (default: structure.mmd)
  --depth=<n>           # Maximum depth to analyze (default: unlimited)

Example:
  ./scripts/analysis/analyze_structure.sh --exclude-dirs=target,node_modules --output=project.mmd
```

## Documentation Scripts

### document_structure.sh

Generates documentation for project structure including memory bank and source code.

```bash
./scripts/documentation/document_structure.sh [options]

Output Files:
  - logs/documentation/memory_bank_structure.<timestamp>.mmd
  - logs/documentation/source_structure.<timestamp>.mmd
  - logs/documentation/documentation.<timestamp>.log

Features:
  - Generates Mermaid diagrams for memory bank structure
  - Generates Mermaid diagrams for source code structure
  - Validates diagram syntax
  - Provides detailed logging
```

## Testing Scripts

### test_unified_db_runner.sh

Tests the UnifiedDatabaseRunner against Oracle database.

```bash
# Run all tests automatically
./scripts/testing/test_unified_db_runner.sh

# Run tests interactively
./scripts/testing/test_unified_db_runner.sh --interactive
```

#### Interactive Mode Features

- Run tests one at a time
- View detailed test output in a paginated format
- Choose which tests to run
- See test descriptions before running

#### Interactive Mode Usage

1. Start interactive mode:

```bash
./scripts/testing/test_unified_db_runner.sh --interactive
```

2. Navigate the menu:

```bash
   Available Tests:
   1) Basic SQL execution
      Description: Demonstrates basic SQL execution including DDL and DML statements

   2) SQL with print statements
      Description: Shows how to enable SQL statement printing for debugging

   3) SQL with auto-commit
      Description: Demonstrates auto-commit mode for immediate changes

   4) Stored procedure
      Description: Shows how to execute stored procedures with parameters

   5) Function execution
      Description: Demonstrates function execution with return value

   6) Run all tests
   7) Exit
```

3. Test Output Navigation:
   - Use arrow keys to scroll through test output
   - Press 'q' to exit the output viewer
   - Press '/' to search within the output
   - Press 'n' to find next search match
   - Press 'N' to find previous search match

4. Output Files:
   - Complete test history: `logs/testing/unified_db_runner/test_run.<timestamp>.log`
   - Current test output: `logs/testing/unified_db_runner/current_test.log`

#### Test Types and Examples

1. **Basic SQL Execution**

```bash
# Example output shows:
- Table creation
- Data insertion
- Query execution
   ```

2. **SQL with Print Statements**

```bash
# Shows SQL statements before execution:
- CREATE TABLE statements
- INSERT statements
- SELECT statements
```

3. **SQL with Auto-commit**

```bash
# Demonstrates:
- Immediate data persistence
- Transaction handling
```

4. **Stored Procedure Execution**

```bash
# Tests stored procedures with:
- Input parameters
- Output parameters
- Error handling
```

5. **Function Execution**

```bash
# Shows function calls with:
- Return values
- Parameter passing
- Error scenarios
```

#### Troubleshooting Interactive Mode

1. **No Output Displayed**
   - Check if less is installed: `which less`
   - Verify log directory permissions
   - Check available disk space

2. **Color Issues**
   - Ensure terminal supports ANSI colors
   - Try running with TERM=xterm-256color

3. **Navigation Issues**
   - Ensure LESS environment variable is set correctly
   - Try setting: `export LESS="-R"`

4. **Common Error Messages**

```bash
# Database connection failed
- Verify Docker container is running
- Check database credentials

# Permission denied
- Check file permissions
- Verify script is executable

# Invalid test selection
- Enter only numbers within the displayed range
```

### test_manager.sh

Manages and executes different types of tests.

```bash
./scripts/testing/test_manager.sh [options]

Test Types:
  - Unit Tests
  - Integration Tests
  - Memory Bank Tests

Output:
  - logs/testing/unit_tests.<timestamp>.log
  - logs/testing/integration_tests.<timestamp>.log
  - logs/testing/memory_bank_tests.<timestamp>.log
```

### test_memory_bank.sh

Validates memory bank structure and content.

```bash
./scripts/testing/test_memory_bank.sh

Checks:
  - Required files existence
  - File structure compliance
  - Front matter validation
  - Content format validation
```

## Validation Scripts

### validate_output.sh

Validates command outputs and file changes.

```bash
./scripts/validation/validate_output.sh [options]

Features:
  - Command output validation
  - File change validation
  - Directory structure validation
  - Detailed logging of validation results

Output:
  - logs/validation/command_validation.<timestamp>.log
  - logs/validation/file_validation.<timestamp>.log
  - logs/validation/directory_validation.<timestamp>.log
```

## Template Scripts

### process_template.sh

Processes templates for memory bank and documentation.

```bash
./scripts/templates/process_template.sh <template_name> <output_file>

Available Templates:
  - current_state.mdc
  - base.mdc
  - document.mdc
  - README.mdc

Example:
  ./scripts/templates/process_template.sh current_state.mdc memory-bank/current_state.mdc
```

## Common Usage Patterns

1. **Full Project Analysis**:

```bash
./scripts/analysis/analyze_codebase.sh structure
./scripts/documentation/document_structure.sh
```

2. **Run All Tests**:

```bash
./scripts/testing/test_manager.sh
```

3. **Database Testing**:

```bash
./scripts/testing/test_unified_db_runner.sh
   ```

4. **Documentation Update**:

```bash
./scripts/documentation/document_structure.sh
```

## Logging

All scripts use a consistent logging structure:

- Logs are stored in `logs/<category>/<script_name>.<timestamp>.log`
- Log levels: INFO, DEBUG, ERROR, SUCCESS
- Color-coded output for better visibility

## Error Handling

Scripts follow these error handling practices:

1. Non-zero exit codes for failures
2. Detailed error messages in logs
3. Cleanup on failure when appropriate
4. Validation of inputs and prerequisites

## Prerequisites

- Bash shell
- Docker (for database tests)
- Java/Maven (for Java-related scripts)
- Git (for version control related features)
