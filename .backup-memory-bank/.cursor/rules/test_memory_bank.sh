#!/bin/bash

# Memory Bank Test Suite

# Load configuration
CONFIG_FILE=".cursor/rules/memory-bank/test_config.mdc"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ Missing test configuration file: $CONFIG_FILE"
    exit 1
fi

# Parse YAML configuration (simplified)
parse_yaml() {
    local prefix=$2
    local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
    sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p" "$1" |
    awk -F$fs '{
        indent = length($1)/2;
        vname[indent] = $2;
        for (i in vname) {if (i > indent) {delete vname[i]}}
        if (length($3) > 0) {
            vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
            printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
        }
    }'
}

# Load test groups
eval $(parse_yaml "$CONFIG_FILE" "test_")

# Test 1: Required Directories
test_directories() {
    required_dirs=(
        ".cursor/rules/templates/memory-bank"
        ".cursor/rules/templates/scripts"
        ".cursor/current/scripts"
        ".cursor/memory-bank"
        "memory-bank/core"
        "memory-bank/active"
        "memory-bank/product"
        "memory-bank/database"
    )
    
    for dir in "${required_dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            echo "❌ Missing directory: $dir"
            return 1
        fi
    done
    echo "✅ All required directories present"
}

# Test 2: Required Template Files
test_templates() {
    required_templates=(
        ".cursor/rules/templates/memory-bank/README.mdc"
        ".cursor/rules/templates/memory-bank/current_state.mdc"
        ".cursor/rules/templates/scripts/current_script.mdc"
    )
    
    for template in "${required_templates[@]}"; do
        if [ ! -f "$template" ]; then
            echo "❌ Missing template: $template"
            return 1
        fi
    done
    echo "✅ All required templates present"
}

# Test 3: Required Core Files
test_core_files() {
    required_files=(
        "memory-bank/README.mdc"
        "memory-bank/core/projectbrief.mdc"
        "memory-bank/core/systemPatterns.mdc"
        "memory-bank/core/techContext.mdc"
        "memory-bank/active/activeContext.mdc"
        "memory-bank/active/progress.mdc"
        "memory-bank/product/productContext.mdc"
        "memory-bank/database/patterns.mdc"
        "memory-bank/database/evolution.mdc"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "$file" ]; then
            echo "❌ Missing core file: $file"
            return 1
        fi
    done
    echo "✅ All required core files present"
}

# Test 4: Template Structure
test_template_structure() {
    templates=(
        ".cursor/rules/templates/memory-bank/README.mdc"
        ".cursor/rules/templates/memory-bank/current_state.mdc"
        ".cursor/rules/templates/scripts/current_script.mdc"
    )
    
    for template in "${templates[@]}"; do
        if ! grep -q "^---$" "$template" || ! grep -q "description:" "$template"; then
            echo "❌ Invalid template structure in: $template"
            return 1
        fi
    done
    echo "✅ All templates have correct structure"
}

# Test 5: Current State
test_current_state() {
    state_file=".cursor/current/state.mdc"
    required_sections=(
        "Active Configuration"
        "Current Structure"
        "Active Targets"
        "Current Patterns"
        "Active Issues/Tasks"
        "Recent Changes"
        "Next Steps"
        "Quick Reference"
    )
    
    for section in "${required_sections[@]}"; do
        if ! grep -q "^## $section" "$state_file"; then
            echo "❌ Missing section in current state: $section"
            return 1
        fi
    done
    echo "✅ Current state file has all required sections"
}

# Test 6: Script State
test_script_state() {
    script_dirs=(
        ".cursor/current/scripts/oracle"
        ".cursor/current/scripts/postgres"
    )
    
    for dir in "${script_dirs[@]}"; do
        if [ ! -f "$dir/current.sql" ] || [ ! -d "$dir/history" ]; then
            echo "❌ Invalid script structure in: $dir"
            return 1
        fi
    done
    echo "✅ Script state structure is valid"
}

# Test 7: File Extensions
test_file_extensions() {
    # Memory bank files should be .mdc
    if find memory-bank/ -name "*.md" -not -name "README.md" | grep -q .; then
        echo "❌ Found .md files instead of .mdc in memory-bank/"
        return 1
    fi
    
    # Script files should be .sql
    if find .cursor/current/scripts/ -type f -not -name "*.sql" -not -name "*.mdc" | grep -q .; then
        echo "❌ Found non-SQL files in scripts directory"
        return 1
    fi
    
    echo "✅ File extensions are correct"
}

# Test 8: History Maintenance
test_history_maintenance() {
    script_dirs=(
        ".cursor/current/scripts/oracle/history"
        ".cursor/current/scripts/postgres/history"
    )
    
    for dir in "${script_dirs[@]}"; do
        if [ ! -d "$dir" ] || [ -z "$(ls -A $dir 2>/dev/null)" ]; then
            echo "❌ Missing or empty history in: $dir"
            return 1
        fi
    done
    echo "✅ Script history is maintained"
}

# Test 9: Recent Changes
test_recent_changes() {
    changes_file=".cursor/memory-bank/code_history.mdc"
    if [ ! -f "$changes_file" ] || ! grep -q "^## Recent Changes" "$changes_file"; then
        echo "❌ Recent changes not properly tracked"
        return 1
    fi
    echo "✅ Recent changes are tracked"
}

# Run specific test group
run_test_group() {
    local group=$1
    local tests=($(echo $test_${group} | tr ',' ' '))
    
    echo "Running $group tests..."
    echo "=========================="
    
    failed=0
    for test in "${tests[@]}"; do
        echo -n "Running $test: "
        if ! $test; then
            failed=$((failed + 1))
        fi
    done
    
    echo "=========================="
    if [ $failed -eq 0 ]; then
        echo "✅ All $group tests passed!"
        return 0
    else
        echo "❌ $failed test(s) failed in $group!"
        return 1
    fi
}

# Main execution
if [ "$1" ]; then
    # Run specific test group
    case "$1" in
        "full_suite"|"quick_checks"|"critical_checks")
            run_test_group "$1"
            ;;
        *)
            # Run specific test
            $1
            ;;
    esac
else
    # Run quick checks by default
    run_test_group "quick_checks"
fi 