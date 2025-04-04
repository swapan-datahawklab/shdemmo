#!/bin/bash

# Exit on error, undefined variables, and pipe failures
set -euo pipefail

# Constants
readonly SUCCESS=0
readonly ERROR=1

# Colors for output (will be disabled on Windows if not supported)
if [ -t 1 ]; then # Check if stdout is a terminal
    readonly RED='\033[0;31m'
    readonly GREEN='\033[0;32m'
    readonly YELLOW='\033[1;33m'
    readonly BLUE='\033[0;34m'
    readonly CYAN='\033[0;36m'
    readonly NC='\033[0m' # No Color
else
    readonly RED=''
    readonly GREEN=''
    readonly YELLOW=''
    readonly BLUE=''
    readonly CYAN=''
    readonly NC=''
fi

# Platform detection
readonly OS_NAME=$(uname -s)
readonly IS_WINDOWS=$(echo "${OS_NAME}" | grep -i "MINGW\|CYGWIN\|MSYS" > /dev/null && echo "true" || echo "false")
readonly BUNDLE_NAME_WINDOWS="shdemmo-bundle-windows"
readonly BUNDLE_NAME_LINUX="shdemmo-bundle-linux"

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# Function to clean up previous builds and distributions
cleanup() {
    log_info "Cleaning up previous builds and distributions..."
    rm -rf "${BUNDLE_NAME_WINDOWS}" "${BUNDLE_NAME_LINUX}"
    rm -f "${BUNDLE_NAME_WINDOWS}.zip" "${BUNDLE_NAME_LINUX}.tar.gz"
}

# Function to run Maven build
build_application() {
    local start_time
    local end_time
    local duration

    log_info "Building application with Maven..."
    start_time=$(date +%s)

    if ! mvn clean verify > maven.log 2>&1; then
        log_error "Maven build failed. Check maven.log for details."
        cat maven.log >&2
        rm -f maven.log
        return $ERROR
    fi

    end_time=$(date +%s)
    duration=$((end_time - start_time))
    rm -f maven.log
    log_info "Maven build completed successfully in ${duration} seconds"
    return $SUCCESS
}

# Function to extract distribution
extract_distribution() {
    log_info "Extracting distribution..."
    
    local bundle_file
    local bundle_dir
    
    if [ "$IS_WINDOWS" = "true" ]; then
        bundle_file="${BUNDLE_NAME_WINDOWS}.zip"
        bundle_dir="$BUNDLE_NAME_WINDOWS"
        
        if [ ! -f "$bundle_file" ]; then
            log_error "Windows bundle file not found: $bundle_file"
            return $ERROR
        fi
        
        log_info "Extracting Windows bundle..."
        # Remove existing directory if it exists
        rm -rf "$bundle_dir"
        
        # Convert paths to Windows format for PowerShell
        local win_bundle_file=$(cygpath -w "$bundle_file")
        local win_bundle_dir=$(cygpath -w "$bundle_dir")
        
        # Use PowerShell to extract the ZIP file on Windows
        if ! powershell.exe -NoProfile -NonInteractive -Command "
            \$ErrorActionPreference = 'Stop'
            try {
                Expand-Archive -Path '$win_bundle_file' -DestinationPath '$win_bundle_dir' -Force
                exit 0
            } catch {
                Write-Error \$_.Exception.Message
                exit 1
            }
        "; then
            log_error "Failed to extract Windows bundle"
            return $ERROR
        fi
        
        # Verify the extraction by checking for run.bat
        if [ ! -f "${bundle_dir}/run.bat" ]; then
            # Try looking in a subdirectory with the same name
            if [ -f "${bundle_dir}/${bundle_dir}/run.bat" ]; then
                # Move files up one directory
                mv "${bundle_dir}/${bundle_dir}"/* "${bundle_dir}/"
                rm -rf "${bundle_dir:?}/${bundle_dir}"
            else
                log_error "run.bat not found in extracted bundle"
                return $ERROR
            fi
        fi
    else
        bundle_file="${BUNDLE_NAME_LINUX}.tar.gz"
        bundle_dir="$BUNDLE_NAME_LINUX"
        
        if [ ! -f "$bundle_file" ]; then
            log_error "Linux bundle file not found: $bundle_file"
            return $ERROR
        fi
        
        log_info "Extracting Linux bundle..."
        if ! tar -xzf "$bundle_file"; then
            log_error "Failed to extract Linux bundle"
            return $ERROR
        fi
    fi
    
    # Verify extraction
    if [ ! -d "$bundle_dir" ]; then
        log_error "Bundle directory not found after extraction: $bundle_dir"
        return $ERROR
    fi
    
    # Make run script executable
    if [ "$IS_WINDOWS" = "true" ]; then
        chmod +x "${bundle_dir}/run.bat"
    else
        chmod +x "${bundle_dir}/run.sh"
    fi
}

# Function to run a single test
run_test() {
    local test_name="$1"
    local cmd="$2"
    local expected_output="$3"
    local expected_code="$4"
    
    echo -e "\n${BLUE}=== Running Test: $test_name ===${NC}"
    log_info "Command: $cmd"
    log_info "Expected exit code: $expected_code"
    
    local bundle_dir
    if [ "$IS_WINDOWS" = "true" ]; then
        bundle_dir="$BUNDLE_NAME_WINDOWS"
    else
        bundle_dir="$BUNDLE_NAME_LINUX"
    fi
    
    # Change to bundle directory
    if ! cd "$bundle_dir"; then
        log_error "Failed to change to bundle directory: $bundle_dir"
        return $ERROR
    fi
    
    # Run the test
    local output
    local actual_code
    local stderr_output
    
    # Capture both stdout and stderr separately
    if [ "$IS_WINDOWS" = "true" ]; then
        # For Windows, use PowerShell to capture both stdout and stderr
        output=$(powershell.exe -NoProfile -NonInteractive -Command "
            \$ErrorActionPreference = 'Continue'
            \$output = & $cmd 2>&1
            \$exitCode = \$LASTEXITCODE
            Write-Output \$output
            exit \$exitCode
        ") || true
        actual_code=$?
    else
        # For Linux, use a temporary file for stderr
        local stderr_file
        stderr_file=$(mktemp)
        output=$($cmd 2>"$stderr_file") || true
        actual_code=$?
        stderr_output=$(<"$stderr_file")
        rm -f "$stderr_file"
        # Combine stdout and stderr if there was an error
        if [ $actual_code -ne 0 ]; then
            output="${output}${stderr_output}"
        fi
    fi
    
    # Change back to original directory
    cd ..
    
    # Display results
    log_info "Command output:"
    if [ -n "$output" ]; then
        echo "$output" | sed 's/^/    /'
    fi
    log_info "Exit code: $actual_code"
    
    # For invalid mode test, check for error message in output
    if [ "$test_name" = "Invalid Mode" ]; then
        if ! echo "$output" | grep -q "Invalid log mode" && [ $actual_code -eq 0 ]; then
            log_error "Invalid mode test failed: Expected error message about invalid log mode"
            return $ERROR
        fi
    else
        # For other tests, verify the expected output is present
        if [ -n "$expected_output" ] && ! echo "$output" | grep -q "$expected_output"; then
            log_error "Expected output not found: $expected_output"
            return $ERROR
        fi
    fi
    
    # Verify exit code
    if [ "$actual_code" != "$expected_code" ]; then
        # Special case for invalid mode on Windows where PowerShell might affect the exit code
        if [ "$test_name" = "Invalid Mode" ] && [ "$IS_WINDOWS" = "true" ] && [ $actual_code -ne 0 ]; then
            log_info "Accepting non-zero exit code for invalid mode test on Windows"
        else
            log_error "Exit code mismatch. Expected: $expected_code, Got: $actual_code"
            return $ERROR
        fi
    fi
    
    log_info "Test passed successfully"
    return $SUCCESS
}

# Function to run all tests
run_tests() {
    log_info "Running tests..."
    
    local exit_code=$SUCCESS
    local bundle_dir
    if [ "$IS_WINDOWS" = "true" ]; then
        bundle_dir="$BUNDLE_NAME_WINDOWS"
        run_cmd="./run.bat"
    else
        bundle_dir="$BUNDLE_NAME_LINUX"
        run_cmd="./run.sh"
    fi
    
    # Define test cases
    declare -A tests=(
        ["Default Mode"]="$run_cmd|Hello, World!|0"
        ["Debug Mode"]="$run_cmd -l debug|DEBUG|0"
        ["Trace Mode"]="$run_cmd -l trace|TRACE|0"
        ["Quiet Mode"]="$run_cmd -l quiet|Hello, World!|0"
        ["Help"]="$run_cmd --help|Usage:|0"
        ["Name Argument"]="$run_cmd -n \"Test User\"|Hello, Test User!|0"
    )
    
    # Run each test
    for test_name in "${!tests[@]}"; do
        IFS='|' read -r cmd expected expected_code <<< "${tests[$test_name]}"
        if ! run_test "$test_name" "$cmd" "$expected" "$expected_code"; then
            log_error "Test failed: $test_name"
            exit_code=$ERROR
            break
        fi
    done
    
    return $exit_code
}

# Main function
main() {
    local exit_code=$SUCCESS
    
    # Print platform information
    log_info "Running on: ${OS_NAME}"
    if [ "$IS_WINDOWS" = "true" ]; then
        log_info "Detected Windows environment (Git Bash)"
    else
        log_info "Detected Unix environment"
    fi
    
    # Execute steps
    cleanup || exit_code=$ERROR
    
    if [ $exit_code -eq $SUCCESS ]; then
        build_application || exit_code=$ERROR
    fi
    
    if [ $exit_code -eq $SUCCESS ]; then
        extract_distribution || exit_code=$ERROR
    fi
    
    if [ $exit_code -eq $SUCCESS ]; then
        run_tests || exit_code=$ERROR
    fi
    
    # Final cleanup
    if [ $exit_code -eq $SUCCESS ]; then
        cleanup
        log_info "All tests completed successfully!"
    else
        log_error "Tests failed!"
    fi
    
    return $exit_code
}

# Run main function
main "$@" 