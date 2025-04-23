#!/bin/bash

# Get the project root directory (where the script is run from)
PROJECT_ROOT="$(pwd)"

# Create logs directory if it doesn't exist
LOGS_DIR="${PROJECT_ROOT}/logs/analysis"
mkdir -p "${LOGS_DIR}"

# Timestamp for log files
TIMESTAMP=$(date +%Y%m%d.%S)

# Analysis output files
STRUCTURE_ANALYSIS="${LOGS_DIR}/structure_analysis.${TIMESTAMP}.log"
PATTERN_ANALYSIS="${LOGS_DIR}/pattern_analysis.${TIMESTAMP}.log"
DEPENDENCY_ANALYSIS="${LOGS_DIR}/dependency_analysis.${TIMESTAMP}.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to analyze project structure
analyze_structure() {
    echo "Analyzing project structure..."
    {
        echo "Project Structure Analysis - $(date '+%Y-%m-%d %H:%M:%S')"
        echo "=================================="
        echo
        echo "Directory Structure:"
        tree -L 4 --dirsfirst "${PROJECT_ROOT}" -I "node_modules|target|.git"
        echo
        echo "File Statistics:"
        echo "--------------"
        echo "Java Files: $(find "${PROJECT_ROOT}" -name "*.java" | wc -l)"
        echo "Test Files: $(find "${PROJECT_ROOT}" -name "*Test.java" | wc -l)"
        echo "Memory Bank Files: $(find "${PROJECT_ROOT}" -name "*.mdc" | wc -l)"
        echo "Configuration Files: $(find "${PROJECT_ROOT}" -name "*.properties" -o -name "*.yml" -o -name "*.yaml" | wc -l)"
        echo
    } > "${STRUCTURE_ANALYSIS}"
    echo -e "${GREEN}Structure analysis completed: ${STRUCTURE_ANALYSIS}${NC}"
}

# Function to analyze code patterns
analyze_patterns() {
    echo "Analyzing code patterns..."
    {
        echo "Code Pattern Analysis - $(date '+%Y-%m-%d %H:%M:%S')"
        echo "==============================="
        echo
        echo "Common Patterns:"
        echo "--------------"
        echo "Controllers:"
        find "${PROJECT_ROOT}" -type f -name "*Controller.java" -exec grep -l "@RestController\|@Controller" {} \;
        echo
        echo "Services:"
        find "${PROJECT_ROOT}" -type f -name "*Service.java" -exec grep -l "@Service" {} \;
        echo
        echo "Repositories:"
        find "${PROJECT_ROOT}" -type f -name "*Repository.java" -exec grep -l "@Repository" {} \;
        echo
        echo "Test Classes:"
        find "${PROJECT_ROOT}" -type f -name "*Test.java" -exec grep -l "@Test" {} \;
        echo
    } > "${PATTERN_ANALYSIS}"
    echo -e "${GREEN}Pattern analysis completed: ${PATTERN_ANALYSIS}${NC}"
}

# Function to analyze dependencies
analyze_dependencies() {
    echo "Analyzing project dependencies..."
    {
        echo "Dependency Analysis - $(date '+%Y-%m-%d %H:%M:%S')"
        echo "=============================="
        echo
        if [ -f "${PROJECT_ROOT}/pom.xml" ]; then
            echo "Maven Dependencies:"
            echo "-----------------"
            mvn dependency:tree -DoutputFile="${DEPENDENCY_ANALYSIS}.temp"
            cat "${DEPENDENCY_ANALYSIS}.temp" >> "${DEPENDENCY_ANALYSIS}"
            rm "${DEPENDENCY_ANALYSIS}.temp"
        fi
        if [ -f "${PROJECT_ROOT}/build.gradle" ]; then
            echo "Gradle Dependencies:"
            echo "------------------"
            gradle dependencies > "${DEPENDENCY_ANALYSIS}.temp"
            cat "${DEPENDENCY_ANALYSIS}.temp" >> "${DEPENDENCY_ANALYSIS}"
            rm "${DEPENDENCY_ANALYSIS}.temp"
        fi
        echo
    } > "${DEPENDENCY_ANALYSIS}"
    echo -e "${GREEN}Dependency analysis completed: ${DEPENDENCY_ANALYSIS}${NC}"
}

# Main execution
echo "Starting project analysis at $(date '+%Y-%m-%d %H:%M:%S')"
echo "=================================================="
echo

analyze_structure
analyze_patterns
analyze_dependencies

echo
echo "Analysis Summary:"
echo "---------------"
echo "1. Structure Analysis: ${STRUCTURE_ANALYSIS}"
echo "2. Pattern Analysis: ${PATTERN_ANALYSIS}"
echo "3. Dependency Analysis: ${DEPENDENCY_ANALYSIS}"
echo
echo "Analysis completed at $(date '+%Y-%m-%d %H:%M:%S')" 