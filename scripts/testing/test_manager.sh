#!/bin/bash

# Colors for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test script registry
declare -A TEST_SCRIPTS=(
    ["project_structure"]="test_project_structure_compliance.sh:Validates project structure compliance"
    ["output_evaluation"]="test_output_evaluation.sh:Evaluates command outputs and changes"
    ["memory_bank"]="test_memory_bank.sh:Tests memory bank structure and content"
)

# Function to print section headers
print_header() {
    echo -e "\n${YELLOW}=== $1 ===${NC}\n"
}

# Function to list all test scripts
list_tests() {
    print_header "Available Test Scripts"
    
    echo -e "${BLUE}Test Name${NC}\t\t${BLUE}Description${NC}"
    echo "----------------------------------------"
    
    for test_name in "${!TEST_SCRIPTS[@]}"; do
        IFS=':' read -r script_file description <<< "${TEST_SCRIPTS[$test_name]}"
        echo -e "${GREEN}$test_name${NC}\t\t$description"
        echo "Script: .cursor/rules/tests/$script_file"
        echo "----------------------------------------"
    done
}

# Function to run a specific test
run_test() {
    local test_name="$1"
    shift # Remove the test name from arguments
    
    if [[ -z "${TEST_SCRIPTS[$test_name]}" ]]; then
        echo -e "${RED}Error: Test '$test_name' not found${NC}"
        echo "Available tests:"
        list_tests
        return 1
    fi
    
    IFS=':' read -r script_file description <<< "${TEST_SCRIPTS[$test_name]}"
    local script_path=".cursor/rules/tests/$script_file"
    
    if [[ ! -f "$script_path" ]]; then
        echo -e "${RED}Error: Script file not found: $script_path${NC}"
        return 1
    fi
    
    if [[ ! -x "$script_path" ]]; then
        chmod +x "$script_path"
    fi
    
    print_header "Running Test: $test_name"
    echo -e "Description: $description"
    echo -e "Script: $script_path"
    echo -e "Arguments: $@"
    echo "----------------------------------------"
    
    "$script_path" "$@"
}

# Function to run all tests
run_all_tests() {
    print_header "Running All Tests"
    
    local failed_tests=()
    
    for test_name in "${!TEST_SCRIPTS[@]}"; do
        echo -e "\n${YELLOW}Running test: $test_name${NC}"
        if ! run_test "$test_name" "$@"; then
            failed_tests+=("$test_name")
        fi
    done
    
    if [ ${#failed_tests[@]} -eq 0 ]; then
        echo -e "\n${GREEN}All tests passed successfully!${NC}"
        return 0
    else
        echo -e "\n${RED}The following tests failed:${NC}"
        for test in "${failed_tests[@]}"; do
            echo -e "${RED}- $test${NC}"
        done
        return 1
    fi
}

# Function to verify test script structure
verify_test_structure() {
    print_header "Verifying Test Script Structure"
    
    local all_valid=true
    
    # Check test directory exists
    if [[ ! -d ".cursor/rules/tests" ]]; then
        echo -e "${RED}Error: Test directory not found: .cursor/rules/tests${NC}"
        return 1
    fi
    
    # Verify each registered test script
    for test_name in "${!TEST_SCRIPTS[@]}"; do
        IFS=':' read -r script_file description <<< "${TEST_SCRIPTS[$test_name]}"
        local script_path=".cursor/rules/tests/$script_file"
        
        echo -n "Checking $test_name ($script_file)... "
        
        if [[ ! -f "$script_path" ]]; then
            echo -e "${RED}Not found${NC}"
            all_valid=false
            continue
        fi
        
        if [[ ! -x "$script_path" ]]; then
            echo -e "${YELLOW}Not executable${NC}"
            chmod +x "$script_path"
            echo -e "${GREEN}Fixed${NC}"
        else
            echo -e "${GREEN}Valid${NC}"
        fi
    done
    
    if [[ "$all_valid" = true ]]; then
        echo -e "\n${GREEN}All test scripts are properly structured${NC}"
        return 0
    else
        echo -e "\n${RED}Some test scripts are missing or invalid${NC}"
        return 1
    fi
}

# Show usage if no arguments provided
if [ "$#" -eq 0 ]; then
    print_header "Test Manager Usage"
    echo "Usage: $0 <command> [args...]"
    echo
    echo "Commands:"
    echo "  list                    List all available tests"
    echo "  run <test_name> [args]  Run a specific test"
    echo "  run-all [args]          Run all tests"
    echo "  verify                  Verify test script structure"
    echo
    echo "Example:"
    echo "  $0 run project_structure src/main/java/NewFile.java"
    echo "  $0 run-all"
    echo "  $0 list"
    exit 1
fi

# Parse command
command="$1"
shift

case "$command" in
    "list")
        list_tests
        ;;
    "run")
        if [ "$#" -eq 0 ]; then
            echo -e "${RED}Error: No test name provided${NC}"
            echo "Available tests:"
            list_tests
            exit 1
        fi
        run_test "$@"
        ;;
    "run-all")
        run_all_tests "$@"
        ;;
    "verify")
        verify_test_structure
        ;;
    *)
        echo -e "${RED}Error: Unknown command '$command'${NC}"
        echo "Available commands: list, run, run-all, verify"
        exit 1
        ;;
esac 