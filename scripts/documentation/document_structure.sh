#!/bin/bash

# Get the project root directory (where the script is run from)
PROJECT_ROOT="$(pwd)"

# Create logs directory if it doesn't exist
LOGS_DIR="${PROJECT_ROOT}/logs/documentation"
mkdir -p "${LOGS_DIR}"

# Timestamp for log files
TIMESTAMP=$(date +%Y%m%d.%S)

# Documentation output files
MEMORY_BANK_DIAGRAM="${LOGS_DIR}/memory_bank_structure.${TIMESTAMP}.mmd"
SOURCE_DIAGRAM="${LOGS_DIR}/source_structure.${TIMESTAMP}.mmd"
DOCUMENTATION_LOG="${LOGS_DIR}/documentation.${TIMESTAMP}.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to generate Mermaid diagram header
generate_header() {
    local output_file="$1"
    local title="$2"
    
    cat > "$output_file" << EOF
---
title: $title
---
flowchart TD
    classDef default fill:#f9f9f9,stroke:#333,stroke-width:2px
    classDef root fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef directory fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef file fill:#fff3e0,stroke:#e65100,stroke-width:2px
EOF
}

# Function to generate nodes for a directory
generate_nodes() {
    local dir="$1"
    local parent_id="$2"
    local output_file="$3"
    local prefix="$4"
    
    # Process directories first
    find "$dir" -maxdepth 1 -mindepth 1 -type d | while read -r subdir; do
        local name=$(basename "$subdir")
        
        # Skip excluded directories
        if [[ "$name" == "target" || "$name" == "node_modules" || "$name" == ".git" || 
              ("$name" == .* && "$name" != ".cursor") ]]; then
            continue
        fi
        
        local node_id="${prefix}_${name}"
        echo "    ${parent_id}[\"${parent_id}\"] --> ${node_id}[\"${name}/\"]" >> "$output_file"
        echo "    class ${node_id} directory" >> "$output_file"
        
        # Recursively process subdirectory
        generate_nodes "$subdir" "$node_id" "$output_file" "${node_id}"
    done
    
    # Then process files
    find "$dir" -maxdepth 1 -mindepth 1 -type f | while read -r file; do
        local name=$(basename "$file")
        local node_id="${prefix}_${name}"
        echo "    ${parent_id}[\"${parent_id}\"] --> ${node_id}[\"${name}\"]" >> "$output_file"
        echo "    class ${node_id} file" >> "$output_file"
    done
}

# Function to document memory bank structure
document_memory_bank() {
    echo "Documenting memory bank structure..."
    
    generate_header "$MEMORY_BANK_DIAGRAM" "Memory Bank Structure"
    
    # Document .cursor/memory-bank
    echo "    root[\".cursor/memory-bank\"]" >> "$MEMORY_BANK_DIAGRAM"
    echo "    class root root" >> "$MEMORY_BANK_DIAGRAM"
    generate_nodes "${PROJECT_ROOT}/.cursor/memory-bank" "root" "$MEMORY_BANK_DIAGRAM" "cursor"
    
    # Document memory-bank
    echo "    mb_root[\"memory-bank\"]" >> "$MEMORY_BANK_DIAGRAM"
    echo "    class mb_root root" >> "$MEMORY_BANK_DIAGRAM"
    generate_nodes "${PROJECT_ROOT}/memory-bank" "mb_root" "$MEMORY_BANK_DIAGRAM" "mb"
    
    echo -e "${GREEN}Memory bank structure documented: ${MEMORY_BANK_DIAGRAM}${NC}"
}

# Function to document source code structure
document_source() {
    echo "Documenting source code structure..."
    
    generate_header "$SOURCE_DIAGRAM" "Source Code Structure"
    
    # Document src directory
    echo "    root[\"src\"]" >> "$SOURCE_DIAGRAM"
    echo "    class root root" >> "$SOURCE_DIAGRAM"
    generate_nodes "${PROJECT_ROOT}/src" "root" "$SOURCE_DIAGRAM" "src"
    
    echo -e "${GREEN}Source code structure documented: ${SOURCE_DIAGRAM}${NC}"
}

# Function to validate Mermaid syntax
validate_mermaid() {
    local diagram_file="$1"
    local title="$2"
    
    if [ ! -f "$diagram_file" ]; then
        echo -e "${RED}Error: Diagram file not found: $diagram_file${NC}"
        return 1
    fi
    
    # Check for basic Mermaid syntax
    if ! grep -q "^flowchart TD" "$diagram_file"; then
        echo -e "${RED}Error: Invalid Mermaid syntax in $title - missing flowchart declaration${NC}"
        return 1
    fi
    
    # Check for node definitions
    if ! grep -q "\[.*\]" "$diagram_file"; then
        echo -e "${RED}Error: Invalid Mermaid syntax in $title - no nodes defined${NC}"
        return 1
    fi
    
    echo -e "${GREEN}Mermaid syntax validation passed for $title${NC}"
    return 0
}

# Main execution
{
    echo "Starting documentation at $(date '+%Y-%m-%d %H:%M:%S')"
    echo "=================================================="
    echo
    
    document_memory_bank
    document_source
    
    echo
    echo "Validating generated diagrams..."
    validate_mermaid "$MEMORY_BANK_DIAGRAM" "Memory Bank Structure"
    validate_mermaid "$SOURCE_DIAGRAM" "Source Code Structure"
    
    echo
    echo "Documentation Summary:"
    echo "-------------------"
    echo "1. Memory Bank Structure: ${MEMORY_BANK_DIAGRAM}"
    echo "2. Source Code Structure: ${SOURCE_DIAGRAM}"
    echo
    echo "Documentation completed at $(date '+%Y-%m-%d %H:%M:%S')"
    
} | tee "$DOCUMENTATION_LOG"

# Make the script executable
chmod +x "$0" 