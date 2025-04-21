#!/bin/bash

# Colors for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test results tracking
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Function to print section headers
print_header() {
    echo -e "\n${YELLOW}=== $1 ===${NC}\n"
}

# Function to check test result
check_test() {
    local test_name="$1"
    local condition="$2"
    
    TESTS_RUN=$((TESTS_RUN + 1))
    
    if eval "$condition"; then
        echo -e "${GREEN}✓ $test_name${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗ $test_name${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# Function to validate file location based on type
validate_file_location() {
    local file_path="$1"
    local file_name=$(basename "$file_path")
    local dir_path=$(dirname "$file_path")
    
    # Memory bank files
    if [[ "$file_name" == *".mdc" ]]; then
        if [[ "$dir_path" != "memory-bank"* && "$dir_path" != ".cursor/memory-bank"* ]]; then
            echo "Memory bank files (.mdc) must be in memory-bank/ or .cursor/memory-bank/"
            return 1
        fi
    fi
    
    # Source files
    if [[ "$file_name" == *".java" ]]; then
        if [[ "$dir_path" != "src/main/java"* && "$dir_path" != "src/test/java"* ]]; then
            echo "Java source files must be in src/main/java/ or src/test/java/"
            return 1
        fi
    fi
    
    return 0
}

# Test: Memory Bank Structure
test_memory_bank_structure() {
    print_header "Testing Memory Bank Structure"
    
    # Check core directories
    check_test "Core memory-bank directory exists" "[ -d 'memory-bank' ]"
    check_test "Core documentation exists" "[ -d 'memory-bank/core' ]"
    check_test "Active state directory exists" "[ -d 'memory-bank/active' ]"
    check_test "Product documentation exists" "[ -d 'memory-bank/product' ]"
    
    # Check essential files
    check_test "Current state file exists" "[ -f 'memory-bank/current_state.mdc' ]"
    check_test "Project brief exists" "[ -f 'memory-bank/core/projectbrief.mdc' ]"
    check_test "Active context exists" "[ -f 'memory-bank/active/activeContext.mdc' ]"
}

# Test: Source Code Structure
test_source_structure() {
    print_header "Testing Source Code Structure"
    
    # Check main source directories
    check_test "Main source directory exists" "[ -d 'src/main/java' ]"
    check_test "Test source directory exists" "[ -d 'src/test/java' ]"
}

# Test: File Extension Compliance
test_file_extensions() {
    print_header "Testing File Extensions"
    
    # Check memory bank files
    local mdc_files_correct=true
    while IFS= read -r -d '' file; do
        if [[ "$file" != *".mdc" ]]; then
            echo -e "${RED}Warning: Memory bank file without .mdc extension: $file${NC}"
            mdc_files_correct=false
        fi
    done < <(find memory-bank -type f -not -name "README.md" -print0)
    
    check_test "Memory bank files use .mdc extension" "[ \"$mdc_files_correct\" = true ]"
}

# Function to evaluate a new file or change
evaluate_change() {
    local file_path="$1"
    print_header "Evaluating Change: $file_path"
    
    # Check if file location is valid
    if ! validate_file_location "$file_path"; then
        echo -e "${RED}Invalid file location${NC}"
        return 1
    fi
    
    # Check if current state needs updating
    if [[ "$file_path" != "memory-bank/current_state.mdc" ]]; then
        echo -e "${YELLOW}Reminder: Update memory-bank/current_state.mdc to reflect this change${NC}"
    fi
    
    # Run all structure tests
    test_memory_bank_structure
    test_source_structure
    test_file_extensions
    
    # Print summary
    print_header "Test Summary"
    echo -e "Tests Run: ${YELLOW}$TESTS_RUN${NC}"
    echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
    
    # Return overall status
    return $((TESTS_FAILED > 0))
}

# Main execution
if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <file_path>"
    echo "Example: $0 src/main/java/com/example/NewFile.java"
    exit 1
fi

evaluate_change "$1" 