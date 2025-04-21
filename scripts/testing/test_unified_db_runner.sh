#!/bin/bash

# Get the project root directory (where the script is run from)
PROJECT_ROOT="$(pwd)"

# Create logs directory if it doesn't exist
LOGS_DIR="${PROJECT_ROOT}/logs/testing/unified_db_runner"
mkdir -p "${LOGS_DIR}"

# Timestamp for log files
TIMESTAMP=$(date +%Y%m%d.%S)
LOG_FILE="${LOGS_DIR}/test_run.${TIMESTAMP}.log"
CURRENT_LOG_FILE="${LOGS_DIR}/current_test.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Oracle DB connection details from docker-compose.yml
DB_HOST="localhost"
DB_PORT=1521
DB_USER="system"
DB_PASS="oracle"
DB_NAME="ORCLPDB1"
JDBC_DRIVER_PATH="/path/to/ojdbc8.jar" # Update this path

# Test files directory
TEST_FILES_DIR="${PROJECT_ROOT}/src/test/resources/sql"
mkdir -p "${TEST_FILES_DIR}"

# Function to log messages with pagination support
log_message() {
    local level="$1"
    local message="$2"
    local details="${3:-}"  # Optional detailed explanation
    local example="${4:-}"  # Optional example
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local output=""
    
    # Build the output string
    output+="${timestamp} [${level}] ${message}\n"
    
    if [ ! -z "$details" ]; then
        output+="\nDetails:\n"
        output+="$(echo "${details}" | sed 's/^/  /')\n"
    fi
    
    if [ ! -z "$example" ]; then
        output+="\nExample:\n"
        output+="$(echo "${example}" | sed 's/^/  /')\n"
    fi
    
    output+="\n---\n"
    
    # Write to both log files
    echo -e "${output}" | tee -a "${LOG_FILE}" "${CURRENT_LOG_FILE}"
}

# Function to display paginated output
display_paginated_output() {
    if [ -f "${CURRENT_LOG_FILE}" ]; then
        less -R "${CURRENT_LOG_FILE}"
        > "${CURRENT_LOG_FILE}"  # Clear the current log after viewing
    fi
}

# Function to check if Oracle container is ready
wait_for_oracle() {
    log_message "INFO" "Waiting for Oracle database to be ready..."
    
    # Get the Oracle container ID
    local container_id=$(docker-compose -f .devcontainer/docker-compose.yml ps -q oracle)
    
    if [ -z "${container_id}" ]; then
        log_message "ERROR" "Oracle container not found"
        return 1
    }
    
    # Wait for the database to be ready
    while ! docker exec "${container_id}" sqlplus -S system/oracle@//localhost:1521/ORCLPDB1 <<< "SELECT 1 FROM DUAL;" > /dev/null 2>&1; do
        log_message "INFO" "Database not ready yet, waiting..."
        sleep 5
    done
    
    log_message "INFO" "Oracle database is ready"
    return 0
}

# Function to create test SQL files
create_test_files() {
    log_message "INFO" "Creating test SQL files..." \
    "We'll create three types of SQL files to test different aspects of the UnifiedDatabaseRunner:
     1. Basic SQL queries and DDL statements
     2. A stored procedure with input/output parameters
     3. A function that returns a value" \
    "Each file type demonstrates different capabilities of the tool:
     - test_query.sql: Shows basic SQL execution
     - test_procedure.sql: Shows stored procedure handling
     - test_function.sql: Shows function execution and return value handling"
    
    # Create a simple query file with documentation
    log_message "INFO" "Creating test_query.sql" \
    "This file demonstrates:
     - Basic SQL query (SELECT FROM DUAL)
     - Table creation (DDL)
     - Data insertion (DML)" \
    "Example usage:
     java -jar unified-db-runner.jar -t oracle -H localhost -P 1521 -u system -p oracle -d ORCLPDB1 test_query.sql"
    
    cat > "${TEST_FILES_DIR}/test_query.sql" << EOF
-- Basic query to test connectivity
SELECT * FROM DUAL;

-- Create a test table with primary key and varchar column
CREATE TABLE test_table (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100)
);

-- Insert test data
INSERT INTO test_table VALUES (1, 'Test Data');
EOF
    
    # Create a stored procedure with documentation
    log_message "INFO" "Creating test_procedure.sql" \
    "This file demonstrates:
     - Creating a stored procedure
     - Handling input parameters
     - Handling output parameters
     - Error handling in PL/SQL" \
    "Example usage:
     java -jar unified-db-runner.jar -t oracle -H localhost -P 1521 -u system -p oracle -d ORCLPDB1 test_proc -i id:NUMBER:1 -o name:VARCHAR2"
    
    cat > "${TEST_FILES_DIR}/test_procedure.sql" << EOF
-- Create a stored procedure that demonstrates parameter handling
CREATE OR REPLACE PROCEDURE test_proc(
    p_id IN NUMBER,
    p_name OUT VARCHAR2
) AS
BEGIN
    -- Input parameter validation
    IF p_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'ID cannot be null');
    END IF;
    
    -- Attempt to fetch the name
    BEGIN
        SELECT name INTO p_name
        FROM test_table
        WHERE id = p_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            p_name := NULL;
            RAISE_APPLICATION_ERROR(-20002, 'No data found for ID: ' || p_id);
    END;
END;
/
EOF
    
    # Create a function with documentation
    log_message "INFO" "Creating test_function.sql" \
    "This file demonstrates:
     - Creating a function
     - Handling input parameters
     - Returning values
     - Error handling in functions" \
    "Example usage:
     java -jar unified-db-runner.jar -t oracle -H localhost -P 1521 -u system -p oracle -d ORCLPDB1 test_func --function --return-type VARCHAR2 -i id:NUMBER:1"
    
    cat > "${TEST_FILES_DIR}/test_function.sql" << EOF
-- Create a function that demonstrates return values and error handling
CREATE OR REPLACE FUNCTION test_func(p_id IN NUMBER)
RETURN VARCHAR2 AS
    v_name VARCHAR2(100);
BEGIN
    -- Input parameter validation
    IF p_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'ID cannot be null');
    END IF;
    
    -- Attempt to fetch and return the name
    BEGIN
        SELECT name INTO v_name
        FROM test_table
        WHERE id = p_id;
        
        RETURN v_name;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'No data found for ID: ' || p_id);
    END;
END;
/
EOF
    
    log_message "INFO" "Test files created successfully" \
    "Created the following files:
     1. ${TEST_FILES_DIR}/test_query.sql
     2. ${TEST_FILES_DIR}/test_procedure.sql
     3. ${TEST_FILES_DIR}/test_function.sql" \
    "You can examine these files to understand the test cases and SQL syntax"
}

# Function to run a single test interactively
run_single_test() {
    local test_name="$1"
    local command="$2"
    local description="$3"
    local expected_output="$4"
    
    # Clear the current log file
    > "${CURRENT_LOG_FILE}"
    
    log_message "INFO" "Running test: ${test_name}" \
    "Test Description:
     ${description}" \
    "Expected Output:
     ${expected_output}"
    
    log_message "DEBUG" "Executing command" \
    "Command details:
     ${command}" \
    "This command demonstrates:
     - How to properly format arguments
     - Required vs optional parameters
     - Proper quoting of values"
    
    if eval "${command}"; then
        log_message "SUCCESS" "Test passed: ${test_name}" \
        "The test completed successfully, demonstrating:
         - Proper command execution
         - Expected output received
         - No errors encountered"
        return 0
    else
        log_message "ERROR" "Test failed: ${test_name}" \
        "Common reasons for failure:
         1. Database connectivity issues
         2. Invalid parameters
         3. SQL syntax errors
         4. Missing permissions
         
         Troubleshooting steps:
         1. Check database connectivity
         2. Verify parameter values
         3. Review SQL syntax
         4. Check user permissions"
        return 1
    fi
}

# Function to display test menu
display_test_menu() {
    echo -e "\n${BLUE}Available Tests:${NC}"
    local i=1
    for test_case in "${test_cases[@]}"; do
        IFS="|" read -r test_name _ description _ <<< "${test_case}"
        echo -e "${i}) ${test_name}"
        echo -e "   ${YELLOW}Description:${NC} ${description}"
        echo
        ((i++))
    done
    echo -e "${i}) Run all tests"
    echo -e "$((i+1))) Exit"
}

# Function to run tests interactively
run_interactive_tests() {
    local failed_tests=0
    local running=true
    
    while $running; do
        display_test_menu
        
        echo -e "\n${BLUE}Choose a test to run (1-$((${#test_cases[@]}+2))):${NC}"
        read -r choice
        
        case $choice in
            ''|*[!0-9]*) 
                echo -e "${RED}Please enter a valid number${NC}"
                continue
                ;;
        esac
        
        if [ $choice -eq $((${#test_cases[@]}+1)) ]; then
            # Run all tests
            for test_case in "${test_cases[@]}"; do
                IFS="|" read -r test_name command description expected <<< "${test_case}"
                if ! run_single_test "${test_name}" "${command}" "${description}" "${expected}"; then
                    ((failed_tests++))
                fi
                display_paginated_output
            done
            running=false
        elif [ $choice -eq $((${#test_cases[@]}+2)) ]; then
            # Exit
            running=false
        elif [ $choice -ge 1 ] && [ $choice -le ${#test_cases[@]} ]; then
            # Run single test
            local test_case="${test_cases[$((choice-1))]}"
            IFS="|" read -r test_name command description expected <<< "${test_case}"
            if ! run_single_test "${test_name}" "${command}" "${description}" "${expected}"; then
                ((failed_tests++))
            fi
            display_paginated_output
            
            echo -e "\n${BLUE}Press Enter to continue...${NC}"
            read -r
        else
            echo -e "${RED}Invalid choice${NC}"
        fi
    done
    
    return $((failed_tests > 0))
}

# Main test execution
main() {
    log_message "INFO" "Starting UnifiedDatabaseRunner tests" \
    "This test suite demonstrates:
     1. Basic SQL execution
     2. Stored procedure calls
     3. Function execution
     4. Parameter handling
     5. Error scenarios
     6. Output formatting" \
    "Prerequisites:
     - Oracle database running in Docker
     - JDBC driver configured
     - Proper permissions set up"
    
    # Wait for Oracle to be ready
    wait_for_oracle || exit 1
    
    # Create test files
    create_test_files
    
    # Array of test cases with documentation
    declare -a test_cases=(
        # Basic SQL file execution
        "Basic SQL execution|java -jar target/unified-db-runner.jar -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} ${TEST_FILES_DIR}/test_query.sql|Demonstrates basic SQL execution including DDL and DML statements|Successful creation of table and data insertion"
        
        # SQL file with print statements
        "SQL with print statements|java -jar target/unified-db-runner.jar -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} --print-statements ${TEST_FILES_DIR}/test_query.sql|Shows how to enable SQL statement printing for debugging|Each SQL statement will be printed before execution"
        
        # SQL file with auto-commit
        "SQL with auto-commit|java -jar target/unified-db-runner.jar -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} --auto-commit ${TEST_FILES_DIR}/test_query.sql|Demonstrates auto-commit mode for immediate changes|Changes will be committed after each statement"
        
        # Stored procedure execution
        "Stored procedure|java -jar target/unified-db-runner.jar -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} test_proc -i id:NUMBER:1 -o name:VARCHAR2|Shows how to execute stored procedures with parameters|Output parameter will contain the retrieved name"
        
        # Function execution
        "Function execution|java -jar target/unified-db-runner.jar -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} test_func --function --return-type VARCHAR2 -i id:NUMBER:1|Demonstrates function execution with return value|Function will return the name for the given ID"
    )
    
    # Check if interactive mode is requested
    if [ "$1" == "--interactive" ]; then
        run_interactive_tests
        exit_code=$?
    else
        # Run all tests in non-interactive mode
        local failed_tests=0
        for test_case in "${test_cases[@]}"; do
            IFS="|" read -r test_name command description expected <<< "${test_case}"
            if ! run_single_test "${test_name}" "${command}" "${description}" "${expected}"; then
                ((failed_tests++))
            fi
            display_paginated_output
        done
        exit_code=$((failed_tests > 0))
    fi
    
    # Print comprehensive summary
    log_message "INFO" "Test execution completed" \
    "Summary:
     - Total tests: ${#test_cases[@]}
     - Failed tests: ${failed_tests}
     - Test files: ${TEST_FILES_DIR}
     - Log file: ${LOG_FILE}
     
     Key Learnings:
     1. How to execute different types of SQL
     2. How to handle stored procedures
     3. How to work with functions
     4. How to use various command options
     5. How to troubleshoot issues
     
     Next Steps:
     1. Review the log file for detailed output
     2. Examine the test files for SQL examples
     3. Try modifying the tests for your needs
     4. Explore additional options in UnifiedDatabaseRunner"
    
    return ${exit_code}
}

# Parse command line arguments
if [ "$1" == "--interactive" ]; then
    echo -e "${BLUE}Running in interactive mode${NC}"
fi

# Execute main function with arguments
main "$@"
exit_code=$?

# Final summary with color
if [ ${exit_code} -eq 0 ]; then
    echo -e "${GREEN}All tests passed successfully${NC}"
else
    echo -e "${RED}Some tests failed. Check the log file: ${LOG_FILE}${NC}"
fi

exit ${exit_code} 