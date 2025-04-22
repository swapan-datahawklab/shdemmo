#!/bin/bash

# Get the project root directory (where the script is run from)
PROJECT_ROOT="$(pwd)"

# Load environment variables from .env file
ENV_FILE="${PROJECT_ROOT}/.devcontainer/.env"
if [ ! -f "${ENV_FILE}" ]; then
    echo -e "\033[0;31m[ERROR] Environment file not found: ${ENV_FILE}\033[0m"
    exit 1
fi

# Source the environment file
set -a  # Automatically export all variables
source "${ENV_FILE}"
set +a  # Stop automatically exporting

# Setup configuration files
setup_config_files() {
    log_message "INFO" "Setting up configuration files..."
    
    # Copy configuration files to the working directory if they don't exist
    for config_file in "application.yaml" "dblist.yaml"; do
        if [ ! -f "${config_file}" ]; then
            if [ -f "src/main/resources/${config_file}" ]; then
                cp "src/main/resources/${config_file}" .
                log_message "INFO" "Copied ${config_file} to working directory"
            else
                log_error "Configuration file not found: src/main/resources/${config_file}"
                return 1
            fi
        fi
    done
    return 0
}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Oracle DB connection details
DB_HOST="localhost"
DB_PORT=1521
DB_USER="system"
DB_PASS="${ORACLE_SYSTEM_PASSWORD}"
DB_NAME="${ORACLE_PDB}"

# Test files directory
TEST_FILES_DIR="${PROJECT_ROOT}/src/test/resources/sql"
mkdir -p "${TEST_FILES_DIR}"

# Function to log messages
log_message() {
    local level="$1"
    local message="$2"
    echo -e "${BLUE}[${level}]${NC} ${message}"
}

# Function to log errors
log_error() {
    local message="$1"
    echo -e "${RED}[ERROR] ${message}${NC}"
}

# Function to check if Oracle container is ready
wait_for_oracle() {
    log_message "INFO" "Checking Oracle database connection..."
    
    local container_id=$(docker compose -f .devcontainer/docker-compose.yml ps -q oracle)
    
    if [ -z "${container_id}" ]; then
        log_error "Oracle container not found"
        return 1
    fi
    
    # Wait for the database to be ready
    local max_attempts=30
    local attempt=1
    while ! docker exec "${container_id}" sqlplus -S system/"${ORACLE_SYSTEM_PASSWORD}"@//localhost:1521/"${ORACLE_PDB}" <<< "SELECT 1 FROM DUAL;" > /dev/null 2>&1; do
        if [ $attempt -ge $max_attempts ]; then
            log_error "Database connection timeout after ${max_attempts} attempts"
            return 1
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    echo ""
    return 0
}

# Function to create test SQL files
create_test_files() {
    log_message "INFO" "Creating test SQL files"
    
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
    
    cat > "${TEST_FILES_DIR}/test_procedure.sql" << EOF
CREATE OR REPLACE PROCEDURE test_proc(
    p_id IN NUMBER,
    p_name OUT VARCHAR2
) AS
BEGIN
    IF p_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'ID cannot be null');
    END IF;
    
    SELECT name INTO p_name
    FROM test_table
    WHERE id = p_id;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002, 'No data found for ID: ' || p_id);
END;
/
EOF
    
    cat > "${TEST_FILES_DIR}/test_function.sql" << EOF
CREATE OR REPLACE FUNCTION test_func(p_id IN NUMBER)
RETURN VARCHAR2 AS
    v_name VARCHAR2(100);
BEGIN
    IF p_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20001, 'ID cannot be null');
    END IF;
    
    SELECT name INTO v_name
    FROM test_table
    WHERE id = p_id;
    RETURN v_name;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002, 'No data found for ID: ' || p_id);
END;
/
EOF
}

# Function to run a single test
run_single_test() {
    local test_name="$1"
    local command="$2"
    
    echo -e "\n${YELLOW}Running Test: ${test_name}${NC}"
    echo -e "${BLUE}Command: ${command}${NC}\n"
    
    local output
    output=$(eval "${command}" 2>&1)
    local status=$?
    
    if [ $status -eq 0 ]; then
        echo -e "${GREEN}✓ Test passed${NC}"
        if [ ! -z "$output" ]; then
            echo "Output:"
            echo "$output"
        fi
        return 0
    else
        echo -e "${RED}✗ Test failed${NC}"
        echo "Error output:"
        echo "$output"
        # Extract ORA- errors
        echo "$output" | grep -E "ORA-[0-9]+" || true
        return 1
    fi
}

# Main test execution
main() {
    if [ -z "$1" ]; then
        log_error "JAR file path not provided"
        echo "Usage: $0 <jar-file-path>"
        exit 1
    fi
    
    local jar_file="$1"
    if [ ! -f "$jar_file" ]; then
        log_error "JAR file not found: $jar_file"
        exit 1
    fi
    
    echo -e "${BLUE}Starting UnifiedDatabaseRunner tests${NC}"
    
    # Setup configuration files
    setup_config_files || exit 1
    
    # Wait for Oracle to be ready
    wait_for_oracle || exit 1
    
    # Create test files
    create_test_files
    
    # Array of test cases
    declare -a test_cases=(
        "Basic SQL execution|java -jar ${jar_file} -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} ${TEST_FILES_DIR}/test_query.sql"
        "SQL with print statements|java -jar ${jar_file} -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} --print-statements ${TEST_FILES_DIR}/test_query.sql"
        "SQL with auto-commit|java -jar ${jar_file} -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} --auto-commit ${TEST_FILES_DIR}/test_query.sql"
        "Stored procedure|java -jar ${jar_file} -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} test_proc -i id:NUMBER:1 -o name:VARCHAR2"
        "Function execution|java -jar ${jar_file} -t oracle -H ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p ${DB_PASS} -d ${DB_NAME} test_func --function --return-type VARCHAR2 -i id:NUMBER:1"
    )
    
    local failed_tests=0
    local total_tests=${#test_cases[@]}
    
    for test_case in "${test_cases[@]}"; do
        IFS="|" read -r test_name command <<< "${test_case}"
        if ! run_single_test "${test_name}" "${command}"; then
            ((failed_tests++))
        fi
    done
    
    # Print test summary
    echo -e "\n${BLUE}Test Summary${NC}"
    echo "------------------------"
    echo -e "Total Tests: ${total_tests}"
    echo -e "Passed: ${GREEN}$((total_tests - failed_tests))${NC}"
    if [ ${failed_tests} -gt 0 ]; then
        echo -e "Failed: ${RED}${failed_tests}${NC}"
    else
        echo -e "Failed: ${failed_tests}"
    fi
    echo "------------------------"
    
    return $((failed_tests > 0))
}

# Execute main function with arguments
main "$@"
exit $? 