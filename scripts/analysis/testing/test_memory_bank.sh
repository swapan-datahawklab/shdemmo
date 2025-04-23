#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function for test results
test_result() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ $1${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ $1${NC}"
        ((TESTS_FAILED++))
    fi
}

echo "Running Memory Bank Tests..."
echo "==========================="

# Test 1: Directory Structure
echo "Testing Directory Structure..."
[ -d ".cursor/rules/templates/memory-bank" ] && [ -d ".cursor/memory-bank" ] && [ -d "memory-bank" ]
test_result "Directory structure exists"

# Test 2: Required Files
echo "Testing Required Files..."
[ -f ".cursor/rules/memory-bank.mdc" ] && [ -f ".cursor/rules/templates/process_template.sh" ]
test_result "Required system files exist"

# Test 3: File Extensions
echo "Testing File Extensions..."
find memory-bank -type f -name "*.mdc" > /dev/null 2>&1
test_result "Memory bank files use .mdc extension"

# Test 4: Template Files
echo "Testing Template Files..."
[ -f ".cursor/rules/templates/memory-bank/current_state.mdc" ] && \
[ -f ".cursor/rules/templates/memory-bank/base.mdc" ] && \
[ -f ".cursor/rules/templates/memory-bank/document.mdc" ] && \
[ -f ".cursor/rules/templates/memory-bank/README.mdc" ]
test_result "Template files exist"

# Test 5: Example Files
echo "Testing Example Files..."
[ -f "memory-bank/active/clean-test.mdc" ] && [ -f "memory-bank/active/template-test.mdc" ]
test_result "Example files exist"

# Test 6: Process Template Script
echo "Testing Process Template Script..."
[ -x ".cursor/rules/templates/process_template.sh" ]
test_result "Process template script is executable"

# Test 7: Content Structure
echo "Testing Content Structure..."
grep -q "description:" memory-bank/active/clean-test.mdc && \
grep -q "description:" memory-bank/active/template-test.mdc
test_result "Files have proper front matter"

# Print Summary
echo "==========================="
echo "Test Summary:"
echo "Passed: $TESTS_PASSED"
echo "Failed: $TESTS_FAILED"
echo "Total: $((TESTS_PASSED + TESTS_FAILED))"

# Exit with status
[ $TESTS_FAILED -eq 0 ] 