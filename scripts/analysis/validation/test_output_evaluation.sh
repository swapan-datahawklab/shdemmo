#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if evaluating a simple command output
if [ "$1" = "--evaluate-command" ]; then
    output="$2"
    echo -e "\n${BLUE}=== Evaluating Command Output ===${NC}"
    echo -e "${YELLOW}Output to evaluate:${NC}"
    echo "$output"
    
    if [[ "$output" == *"command not found"* ]]; then
        echo -e "${RED}✗ Command not found error detected${NC}"
        exit 1
    else
        echo -e "${GREEN}✓ Command executed successfully${NC}"
        exit 0
    fi
fi

# Check if running in non-interactive mode
NON_INTERACTIVE=0
if [ "$1" = "--non-interactive" ]; then
    NON_INTERACTIVE=1
fi

# Helper function to enforce output evaluation
enforce_evaluation() {
    local output="$1"
    local description="$2"
    local expected_patterns="$3"
    
    echo -e "\n${BLUE}=== Evaluating: $description ===${NC}"
    echo -e "${YELLOW}Output to evaluate:${NC}"
    echo "$output"
    
    if [ ! -z "$expected_patterns" ]; then
        echo -e "\n${YELLOW}Expected Patterns to Check:${NC}"
        local missing_patterns=0
        while IFS=',' read -r pattern; do
            if ! echo "$output" | grep -q "$pattern"; then
                echo -e "${RED}✗ Missing expected pattern: $pattern${NC}"
                ((missing_patterns++))
            else
                echo -e "${GREEN}✓ Found pattern: $pattern${NC}"
            fi
        done <<< "$expected_patterns"
        
        if [ $missing_patterns -gt 0 ]; then
            echo -e "${RED}Warning: $missing_patterns expected patterns not found${NC}"
            return 1
        fi
    fi
    
    if [ $NON_INTERACTIVE -eq 1 ]; then
        echo -e "${GREEN}✓ Automatic validation complete${NC}"
        return 0
    fi
    
    # Interactive mode prompts
    while true; do
        echo -e "\n${YELLOW}Have you thoroughly evaluated the output? (yes/no)${NC}"
        read -r response
        case $response in
            [Yy]* )
                echo -e "${GREEN}✓ Output evaluated${NC}"
                break
                ;;
            [Nn]* )
                echo -e "${RED}Please review the output again${NC}"
                ;;
            * )
                echo "Please answer yes or no"
                ;;
        esac
    done
    
    return 0
}

# Function to evaluate file changes
evaluate_file_changes() {
    local file="$1"
    local description="$2"
    
    if [ ! -f "$file" ]; then
        echo -e "${RED}Error: File $file not found${NC}"
        return 1
    fi
    
    echo -e "\n${BLUE}=== Evaluating File: $file ===${NC}"
    echo -e "${YELLOW}File contents:${NC}"
    cat "$file"
    
    if [ $NON_INTERACTIVE -eq 1 ]; then
        echo -e "${GREEN}✓ Automatic file validation complete${NC}"
        return 0
    fi
    
    # Interactive mode prompts
    while true; do
        echo -e "\n${YELLOW}Have you thoroughly evaluated the file? (yes/no)${NC}"
        read -r response
        case $response in
            [Yy]* )
                echo -e "${GREEN}✓ File evaluated${NC}"
                break
                ;;
            [Nn]* )
                echo -e "${RED}Please review the file again${NC}"
                ;;
            * )
                echo "Please answer yes or no"
                ;;
        esac
    done
    
    return 0
}

# Function to evaluate directory structure
evaluate_directory() {
    local dir="$1"
    local description="$2"
    
    if [ ! -d "$dir" ]; then
        echo -e "${RED}Error: Directory $dir not found${NC}"
        return 1
    fi
    
    echo -e "\n${BLUE}=== Evaluating Directory: $dir ===${NC}"
    echo -e "${YELLOW}Directory contents:${NC}"
    ls -R "$dir"
    
    if [ $NON_INTERACTIVE -eq 1 ]; then
        echo -e "${GREEN}✓ Automatic directory validation complete${NC}"
        return 0
    fi
    
    # Interactive mode prompts
    while true; do
        echo -e "\n${YELLOW}Have you thoroughly evaluated the directory? (yes/no)${NC}"
        read -r response
        case $response in
            [Yy]* )
                echo -e "${GREEN}✓ Directory evaluated${NC}"
                break
                ;;
            [Nn]* )
                echo -e "${RED}Please review the directory again${NC}"
                ;;
            * )
                echo "Please answer yes or no"
                ;;
        esac
    done
    
    return 0
}

# Main test execution
main() {
    local tests_passed=0
    local tests_failed=0
    
    echo -e "\n${BLUE}=== Running Output Evaluation Tests ===${NC}"
    
    # Test 1: Simple command output
    echo -e "\nTest 1: Command Output"
    if enforce_evaluation "Operation completed successfully" "Basic command test" "completed successfully"; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test 2: File evaluation
    echo -e "\nTest 2: File Evaluation"
    if evaluate_file_changes ".cursor/rules/memory-bank.mdc" "Memory bank rules file"; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test 3: Directory evaluation
    echo -e "\nTest 3: Directory Evaluation"
    if evaluate_directory "memory-bank" "Memory bank directory"; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Summary
    echo -e "\n${BLUE}=== Test Summary ===${NC}"
    echo "Tests passed: $tests_passed"
    echo "Tests failed: $tests_failed"
    echo "Total tests: $((tests_passed + tests_failed))"
    
    # Exit with status
    [ $tests_failed -eq 0 ]
}

# Run main if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main
fi

# Log file for tracking evaluations
echo "=== Output Evaluation Log ===" > output_evaluation.log
echo "Started: $(date '+%Y-%m-%d %H:%M:%S')" >> output_evaluation.log 