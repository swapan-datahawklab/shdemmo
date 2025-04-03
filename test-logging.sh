#!/bin/bash

# Exit on error, undefined variables, and pipe failures
set -euo pipefail

# Constants
readonly SUCCESS=0
readonly ERROR=1

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Logging functions
log_info() {
    local line_num=""
    if [[ -n "${BASH_LINENO[0]:-}" ]]; then
        line_num="[L${BASH_LINENO[0]}] "
    fi
    echo -e "${GREEN}[INFO]${NC} ${line_num}$1"
}

log_warn() {
    local line_num=""
    if [[ -n "${BASH_LINENO[0]:-}" ]]; then
        line_num="[L${BASH_LINENO[0]}] "
    fi
    echo -e "${YELLOW}[WARN]${NC} ${line_num}$1"
}

log_error() {
    local line_num=""
    if [[ -n "${BASH_LINENO[0]:-}" ]]; then
        line_num="[L${BASH_LINENO[0]}] "
    fi
    echo -e "${RED}[ERROR]${NC} ${line_num}$1" >&2
}

# Function to print remaining tests
print_remaining_tests() {
    local current_test="$1"
    local -n tests_ref="$2"
    local total="${#tests_ref[@]}"
    local remaining=()
    local found_current=0

    echo -e "\n${BLUE}=== Remaining Tests ===${NC}"
    for test in "${test_names[@]}"; do
        if [[ "$test" == "$current_test" ]]; then
            found_current=1
            echo -e "${CYAN}▶ $test${NC} (Current)"
        elif [[ $found_current -eq 1 ]]; then
            echo -e "  $test"
        fi
    done
}

# Function to build the application with Maven
build_application() {
    local start_time
    local end_time
    local duration

    log_info "Building application with Maven..."
    start_time=$(date +%s)

    if ! mvn clean package > maven.log 2>&1; then
        log_error "Maven build failed. Check maven.log for details."
        cat maven.log >&2
        rm maven.log
        return $ERROR
    fi

    end_time=$(date +%s)
    duration=$((end_time - start_time))
    rm maven.log
    log_info "Maven build completed successfully in ${duration} seconds"
    return $SUCCESS
}

# Function to create the application bundle
create_bundle() {
    log_info "Creating application bundle..."
    
    if [[ ! -f "create-bundle.sh" ]]; then
        log_error "create-bundle.sh script not found"
        return $ERROR
    fi

    if ! chmod +x create-bundle.sh; then
        log_error "Failed to make create-bundle.sh executable"
        return $ERROR
    fi

    if ! ./create-bundle.sh > bundle.log 2>&1; then
        log_error "Bundle creation failed. Check bundle.log for details."
        cat bundle.log >&2
        rm bundle.log
        return $ERROR
    fi

    if [[ ! -f "shdemmo-bundle.tar.gz" ]]; then
        log_error "Bundle file not created"
        rm bundle.log
        return $ERROR
    fi

    rm bundle.log
    log_info "Bundle created successfully"
    return $SUCCESS
}

# Function to verify test output and exit code
verify_test() {
    local test_name="$1"
    local expected_output="$2"
    local expected_code="$3"
    shift 3
    local cmd=("$@")

    echo -e "\n${BLUE}=== Testing: $test_name ===${NC}"
    log_info "Running command: ${cmd[*]}"
    log_info "Expected exit code: $expected_code"

    # Run the command and capture both output and exit code
    local output
    local actual_code
    local start_time=$(date +%s.%N)
    
    # Change to the bundle directory to run the test
    if ! pushd shdemmo-bundle > /dev/null; then
        log_error "Failed to change to bundle directory"
        return 1
    fi
    
    # For quiet mode, we need to capture output differently
    if [[ "$test_name" == "Quiet Mode" ]]; then
        local temp_file
        temp_file=$(mktemp)
        "${cmd[@]}" > "$temp_file" 2> /dev/null
        actual_code=$?
        output=$(cat "$temp_file")
        rm -f "$temp_file"
    else
        output=$("${cmd[@]}" 2>&1) || true
        actual_code=$?
    fi
    
    if ! popd > /dev/null; then
        log_error "Failed to return from bundle directory"
        return 1
    fi
    
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc)

    # For debugging quiet mode
    if [[ "$test_name" == "Quiet Mode" ]]; then
        log_info "Raw output before filtering:"
        echo "$output" | cat -A | nl -ba | sed 's/^/    /'
    fi

    # Filter out logback initialization messages and keep only application output
    local filtered_output
    if [[ "$test_name" == "Quiet Mode" ]]; then
        # For quiet mode, get any line containing Hello, World! and clean it up
        filtered_output=$(echo "$output" | grep -a "Hello, World" | tr -d '\r' | tr -d '\0' || true)
    else
        filtered_output=$(echo "$output" | grep -v "|-INFO in" | grep -v "^[0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}" | grep -v "^$" || true)
    fi

    log_info "Command output (filtered):"
    if [[ -n "$filtered_output" ]]; then
        echo "$filtered_output" | nl -ba | sed 's/^/    /'
    else
        log_warn "No output after filtering"
    fi
    log_info "Actual exit code: $actual_code"
    printf "${GREEN}Test duration: %.3f seconds${NC}\n" "$duration"

    # Check exit code
    if [[ $actual_code -ne $expected_code ]]; then
        log_error "Exit code mismatch. Expected: $expected_code, Got: $actual_code"
        return 1
    fi

    # Check output contains expected string
    if [[ -n $expected_output && ! $filtered_output =~ $expected_output ]]; then
        log_error "Expected output not found: $expected_output"
        log_error "Expected: '$expected_output'"
        log_error "Got: '$filtered_output'"
        return 1
    fi

    log_info "Test passed successfully"
    return 0
}

# Function to run a test case
run_test() {
    local test_name="$1"
    local test_spec="$2"
    local -n tests_array="$3"
    local IFS='|'
    read -r cmd expected expected_code <<< "$test_spec"

    # Print remaining tests before running current test
    print_remaining_tests "$test_name" tests_array

    # Split the command into an array, preserving quoted arguments
    local cmd_array=()
    eval "cmd_array=($cmd)" || {
        log_error "Failed to parse command: $cmd"
        return 1
    }

    # Remove the bundle directory prefix since we'll cd into it
    cmd_array[0]="./run.sh"

    verify_test "$test_name" "$expected" "$expected_code" "${cmd_array[@]}"
    return $?
}

# Cleanup function
cleanup() {
    local exit_code=$?
    log_info "Performing cleanup..."
    rm -f maven.log bundle.log run.sh
    if [[ $exit_code -ne 0 ]]; then
        log_warn "Script exited with code: $exit_code"
    fi
}

# Register cleanup function
trap cleanup EXIT

# Main function
main() {
    local exit_code=0

    log_info "=== Starting Logging Test Script ==="

    # Step 1: Copy run.sh and logback.xml from bundle to source
    log_info "Copying configuration files..."
    if [[ ! -f "scripts/run.sh.template" ]]; then
        log_error "run.sh.template not found in scripts directory"
        return 1
    fi
    cp scripts/run.sh.template run.sh || {
        log_error "Failed to copy run.sh.template"
        return 1
    }
    chmod +x run.sh || {
        log_error "Failed to make run.sh executable"
        return 1
    }

    # Step 2: Clean up old bundle
    log_info "Cleaning up old bundle..."
    rm -rf shdemmo-bundle
    rm -f shdemmo-bundle.tar.gz

    # Step 3: Build application
    if ! build_application; then
        return 1
    fi

    # Step 4: Create bundle
    if ! create_bundle; then
        return 1
    fi

    # Step 5: Extract new bundle
    log_info "Extracting bundle..."
    if ! tar xf shdemmo-bundle.tar.gz; then
        log_error "Bundle extraction failed"
        return 1
    fi

    # Make run.sh executable
    log_info "Making run.sh executable..."
    if ! chmod +x shdemmo-bundle/run.sh; then
        log_error "Failed to make run.sh executable"
        return 1
    fi

    log_info "=== Running Tests ==="

    # Array of test cases with expected exit codes
    declare -A tests=(
        ["Default Mode"]='./shdemmo-bundle/run.sh -- |Starting application.*Hello, World!.*Application completed with exit code: 0|0'
        ["Debug Mode"]='./shdemmo-bundle/run.sh -l debug -- |Starting application.*Generated greeting: Hello, World!.*Application completed with exit code: 0|0'
        ["Debug Mode (Short Form)"]='./shdemmo-bundle/run.sh -l debug -- |Starting application.*Generated greeting: Hello, World!.*Application completed with exit code: 0|0'
        ["Trace Mode"]='./shdemmo-bundle/run.sh -l trace -- |Starting application.*Generated greeting: Hello, World!.*Application completed with exit code: 0|0'
        ["Quiet Mode"]='./shdemmo-bundle/run.sh -l quiet -- |Hello, World!|0'
        ["Name Argument"]='./shdemmo-bundle/run.sh -- -n "Test User"|Starting application.*Hello, Test User!.*Application completed with exit code: 0|0'
        ["Debug with Name"]='./shdemmo-bundle/run.sh -l debug -- -n "Test User" --verbose|Starting application.*Generated greeting: Hello, Test User!.*Application completed with exit code: 0|0'
        ["Help"]='./shdemmo-bundle/run.sh --help|Usage: ./run.sh|0'
    )

    # Create an array of test names to ensure consistent order
    declare -a test_names=(
        "Debug Mode (Short Form)"
        "Quiet Mode"
        "Trace Mode"
        "Debug with Name"
        "Debug Mode"
        "Default Mode"
        "Help"
        "Name Argument"
    )

    # Run all tests
    local failed_tests=0
    local total_tests=${#tests[@]}
    local passed_tests=0
    local start_time=$(date +%s)

    for test_name in "${test_names[@]}"; do
        if [[ -n "${tests[$test_name]:-}" ]]; then
            if run_test "$test_name" "${tests[$test_name]}" tests; then
                ((passed_tests++))
            else
                ((failed_tests++))
                log_error "Test '$test_name' failed"
                exit_code=1
            fi
        else
            log_warn "Test '$test_name' not found in test cases"
        fi
    done || true

    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))

    # Print test summary
    echo -e "\n${BLUE}=== Test Summary ===${NC}"
    log_info "Total tests: $total_tests"
    log_info "Passed: $passed_tests"
    if [[ $failed_tests -gt 0 ]]; then
        log_error "Failed: $failed_tests"
    else
        log_info "All tests passed successfully!"
    fi
    log_info "Total duration: ${total_duration} seconds"

    return $exit_code
}

# Run main function and exit with its return code
main "$@"
exit $? 