#!/bin/bash

# Get the project root directory (where the script is run from)
PROJECT_ROOT="$(pwd)"

# Create logs directory if it doesn't exist
LOGS_DIR="${PROJECT_ROOT}/logs/validation"
mkdir -p "${LOGS_DIR}"

# Timestamp for log files
TIMESTAMP=$(date +%Y%m%d.%S)

# Validation output files
COMMAND_LOG="${LOGS_DIR}/command_validation.${TIMESTAMP}.log"
FILE_LOG="${LOGS_DIR}/file_validation.${TIMESTAMP}.log"
DIRECTORY_LOG="${LOGS_DIR}/directory_validation.${TIMESTAMP}.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to enforce output evaluation
enforce_evaluation() {
    local command_output="$1"
    local expected_pattern="$2"
    local error_pattern="$3"
    local description="$4"
    
    {
        echo "Command Output Evaluation - $(date '+%Y-%m-%d %H:%M:%S')"
        echo "Description: ${description}"
        echo "=================================="
        echo
        echo "Command Output:"
        echo "--------------"
        echo "$command_output"
        echo
        echo "Validation Results:"
        echo "------------------"
        
        # Check for expected pattern
        if [ ! -z "$expected_pattern" ]; then
            if echo "$command_output" | grep -q "$expected_pattern"; then
                echo "✓ Found expected pattern: $expected_pattern"
            else
                echo "✗ Missing expected pattern: $expected_pattern"
                return 1
            fi
        fi
        
        # Check for error pattern
        if [ ! -z "$error_pattern" ]; then
            if echo "$command_output" | grep -q "$error_pattern"; then
                echo "✗ Found error pattern: $error_pattern"
                return 1
            else
                echo "✓ No error patterns found"
            fi
        fi
        
    } > "${COMMAND_LOG}"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Command output validation passed${NC}"
        return 0
    else
        echo -e "${RED}Command output validation failed${NC}"
        return 1
    fi
}

# Function to evaluate file changes
evaluate_file_changes() {
    local file_path="$1"
    local required_sections=("$2")
    local forbidden_patterns=("$3")
    
    {
        echo "File Change Evaluation - $(date '+%Y-%m-%d %H:%M:%S')"
        echo "File: ${file_path}"
        echo "=================================="
        echo
        
        if [ ! -f "$file_path" ]; then
            echo "✗ File does not exist: $file_path"
            return 1
        fi
        
        echo "File Content Validation:"
        echo "----------------------"
        
        # Check required sections
        for section in "${required_sections[@]}"; do
            if grep -q "$section" "$file_path"; then
                echo "✓ Found required section: $section"
            else
                echo "✗ Missing required section: $section"
                return 1
            fi
        done
        
        # Check forbidden patterns
        for pattern in "${forbidden_patterns[@]}"; do
            if grep -q "$pattern" "$file_path"; then
                echo "✗ Found forbidden pattern: $pattern"
                return 1
            else
                echo "✓ No forbidden pattern found: $pattern"
            fi
        done
        
    } > "${FILE_LOG}"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}File validation passed${NC}"
        return 0
    else
        echo -e "${RED}File validation failed${NC}"
        return 1
    fi
}

# Function to evaluate directory structure
evaluate_directory() {
    local dir_path="$1"
    local required_files=("$2")
    local required_permissions="$3"
    
    {
        echo "Directory Evaluation - $(date '+%Y-%m-%d %H:%M:%S')"
        echo "Directory: ${dir_path}"
        echo "=================================="
        echo
        
        if [ ! -d "$dir_path" ]; then
            echo "✗ Directory does not exist: $dir_path"
            return 1
        fi
        
        echo "Directory Content Validation:"
        echo "--------------------------"
        
        # Check required files
        for file in "${required_files[@]}"; do
            if [ -f "${dir_path}/${file}" ]; then
                echo "✓ Found required file: $file"
            else
                echo "✗ Missing required file: $file"
                return 1
            fi
        done
        
        # Check permissions if specified
        if [ ! -z "$required_permissions" ]; then
            echo -e "\nPermission Validation:"
            echo "---------------------"
            find "$dir_path" -type f -not -perm "$required_permissions" -exec echo "✗ Incorrect permissions on: {}" \;
            if [ $? -ne 0 ]; then
                return 1
            fi
        fi
        
    } > "${DIRECTORY_LOG}"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Directory validation passed${NC}"
        return 0
    else
        echo -e "${RED}Directory validation failed${NC}"
        return 1
    fi
}

# Example usage
echo "Output Validation Tool"
echo "===================="
echo "Use these functions to validate:"
echo "1. Command output: enforce_evaluation 'output' 'expected' 'error' 'description'"
echo "2. File changes: evaluate_file_changes 'file' 'required' 'forbidden'"
echo "3. Directory structure: evaluate_directory 'dir' 'files' 'permissions'" 