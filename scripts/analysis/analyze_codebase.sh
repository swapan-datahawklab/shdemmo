#!/bin/bash

# Colors for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Find and change to project root (where .git exists)
find_project_root() {
    local current_dir="$PWD"
    while [[ "$PWD" != "/" ]]; do
        if [[ -d ".git" ]]; then
            PROJECT_ROOT="$PWD"
            return 0
        fi
        cd ..
    done
    cd "$current_dir"
    echo -e "${RED}Error: Could not find project root (no .git directory found)${NC}"
    exit 1
}

# Always start from project root
find_project_root
cd "$PROJECT_ROOT"

# Function to print section headers
print_header() {
    echo -e "\n${YELLOW}=== $1 ===${NC}\n"
}

# Function to analyze directory structure
analyze_structure() {
    local dir="${1:-.}"
    local prefix="${2:-}"
    local exclude_pattern="${3:-^$}"  # Default to match nothing
    
    print_header "Project Structure Analysis"
    
    # Find all directories, exclude common build/temp directories
    find "$PROJECT_ROOT" -type d \
        ! -path "*/\.*" \
        ! -path "*/target/*" \
        ! -path "*/build/*" \
        ! -path "*/node_modules/*" \
        ! -path "*/__pycache__/*" \
        -print0 | while IFS= read -r -d '' directory; do
        
        # Skip if matches exclude pattern
        [[ "$directory" =~ $exclude_pattern ]] && continue
        
        # Count files by type in this directory
        local java_count=$(find "$directory" -maxdepth 1 -type f -name "*.java" | wc -l)
        local mdc_count=$(find "$directory" -maxdepth 1 -type f -name "*.mdc" | wc -l)
        local md_count=$(find "$directory" -maxdepth 1 -type f -name "*.md" | wc -l)
        local other_count=$(find "$directory" -maxdepth 1 -type f ! -name "*.java" ! -name "*.mdc" ! -name "*.md" | wc -l)
        
        # Only show directories with files
        if [ $java_count -gt 0 ] || [ $mdc_count -gt 0 ] || [ $md_count -gt 0 ] || [ $other_count -gt 0 ]; then
            echo -e "${BLUE}${directory#$PROJECT_ROOT/}${NC}"
            [ $java_count -gt 0 ] && echo -e "  ${CYAN}Java:${NC} $java_count files"
            [ $mdc_count -gt 0 ] && echo -e "  ${CYAN}Memory Bank:${NC} $mdc_count files"
            [ $md_count -gt 0 ] && echo -e "  ${CYAN}Markdown:${NC} $md_count files"
            [ $other_count -gt 0 ] && echo -e "  ${CYAN}Other:${NC} $other_count files"
            echo
        fi
    done
}

# Function to find similar code
find_similar_code() {
    local pattern="$1"
    local file_pattern="$2"
    
    print_header "Finding Similar Code: $pattern"
    
    # Use ripgrep if available, otherwise fall back to grep
    if command -v rg &> /dev/null; then
        cd "$PROJECT_ROOT" && rg -n -g "$file_pattern" "$pattern" . \
            --no-ignore \
            --hidden \
            --glob '!.git' \
            --glob '!target' \
            --glob '!build' \
            --glob '!node_modules'
    else
        cd "$PROJECT_ROOT" && find . -type f -name "$file_pattern" \
            ! -path "*/\.*" \
            ! -path "*/target/*" \
            ! -path "*/build/*" \
            ! -path "*/node_modules/*" \
            -exec grep -Hn "$pattern" {} \;
    fi
}

# Function to analyze package structure (for Java)
analyze_package_structure() {
    print_header "Package Structure Analysis"
    
    # Find all Java files and extract package declarations
    cd "$PROJECT_ROOT" && find . -type f -name "*.java" ! -path "*/target/*" -print0 | while IFS= read -r -d '' file; do
        package=$(grep -h '^package' "$file" | sed 's/package\s*\([^;]*\);/\1/')
        if [ ! -z "$package" ]; then
            echo -e "${CYAN}Package:${NC} $package"
            echo -e "${BLUE}File:${NC} ${file#./}"
            # Extract and show class/interface declarations
            grep -h '^public \(class\|interface\|enum\)' "$file" | while read -r declaration; do
                echo -e "${GREEN}Declaration:${NC} $declaration"
            done
            echo
        fi
    done
}

# Function to suggest locations for new files
suggest_file_location() {
    local file_name="$1"
    
    print_header "Location Suggestions for: $file_name"
    
    # Extract file extension
    local extension="${file_name##*.}"
    
    case "$extension" in
        java)
            echo -e "${CYAN}Suggested locations for Java files:${NC}"
            echo -e "1. ${GREEN}src/main/java/${NC} - For main source code"
            echo -e "2. ${GREEN}src/test/java/${NC} - For test code"
            # Find similar Java files
            echo -e "\n${CYAN}Similar existing files:${NC}"
            cd "$PROJECT_ROOT" && find . -type f -name "*${file_name##*/}" ! -path "*/target/*" -print0 | \
                while IFS= read -r -d '' file; do
                    echo -e "${BLUE}${file#./}${NC}"
                done
            ;;
        mdc)
            echo -e "${CYAN}Suggested locations for Memory Bank files:${NC}"
            echo -e "1. ${GREEN}memory-bank/${NC} - For permanent memory"
            echo -e "2. ${GREEN}.cursor/memory-bank/${NC} - For temporary memory"
            # Show similar memory bank files
            echo -e "\n${CYAN}Similar existing files:${NC}"
            cd "$PROJECT_ROOT" && find . -type f -name "*.mdc" -print0 | \
                while IFS= read -r -d '' file; do
                    echo -e "${BLUE}${file#./}${NC}"
                done
            ;;
        *)
            echo -e "${YELLOW}No specific suggestions for .$extension files${NC}"
            echo "Please refer to project documentation for guidance"
            ;;
    esac
}

# Function to check for potential duplicates
check_duplicates() {
    local file_name="$1"
    local content_pattern="$2"
    
    print_header "Checking for Potential Duplicates"
    
    # Check for files with similar names
    echo -e "${CYAN}Files with similar names:${NC}"
    cd "$PROJECT_ROOT" && find . -type f -iname "*${file_name%.*}*" ! -path "*/target/*" -print0 | \
        while IFS= read -r -d '' file; do
            echo -e "${BLUE}${file#./}${NC}"
        done
    
    # If content pattern provided, search for similar content
    if [ ! -z "$content_pattern" ]; then
        echo -e "\n${CYAN}Files with similar content:${NC}"
        find_similar_code "$content_pattern" "*"
    fi
}

# Show usage if no arguments provided
if [ "$#" -eq 0 ]; then
    print_header "Project Analysis Usage"
    echo "Usage: $0 <command> [args...]"
    echo
    echo "Commands:"
    echo "  structure                     Show project directory structure"
    echo "  similar <pattern> [file-pat]  Find similar code patterns"
    echo "  packages                      Analyze Java package structure"
    echo "  suggest <file-name>           Suggest location for new file"
    echo "  check <file-name> [pattern]   Check for potential duplicates"
    echo
    echo "Examples:"
    echo "  $0 structure"
    echo "  $0 similar 'class.*Controller' '*.java'"
    echo "  $0 packages"
    echo "  $0 suggest NewService.java"
    echo "  $0 check UserService.java 'interface.*Service'"
    exit 1
fi

# Parse command
command="$1"
shift

case "$command" in
    "structure")
        analyze_structure
        ;;
    "similar")
        if [ "$#" -lt 1 ]; then
            echo -e "${RED}Error: Pattern required${NC}"
            exit 1
        fi
        pattern="$1"
        file_pattern="${2:-*}"
        find_similar_code "$pattern" "$file_pattern"
        ;;
    "packages")
        analyze_package_structure
        ;;
    "suggest")
        if [ "$#" -lt 1 ]; then
            echo -e "${RED}Error: File name required${NC}"
            exit 1
        fi
        suggest_file_location "$1"
        ;;
    "check")
        if [ "$#" -lt 1 ]; then
            echo -e "${RED}Error: File name required${NC}"
            exit 1
        fi
        check_duplicates "$1" "$2"
        ;;
    *)
        echo -e "${RED}Error: Unknown command '$command'${NC}"
        echo "Available commands: structure, similar, packages, suggest, check"
        exit 1
        ;;
esac 