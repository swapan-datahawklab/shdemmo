#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Output files
SOURCE_DIAGRAM="source_structure.mmd"
TEMP_DIAGRAM="temp_structure.mmd"

# Function to generate Mermaid diagram for src directory
generate_source_diagram() {
    local output_file="${1:-$SOURCE_DIAGRAM}"
    echo "flowchart TD" > "$output_file"
    echo "    subgraph Source [Source Code Structure]" >> "$output_file"
    
    # Generate nodes for src directory
    find src -type d | while read -r dir; do
        # Skip the root src directory
        if [ "$dir" = "src" ]; then
            continue
        fi
        
        # Create node name (replace / with _)
        node_name=$(echo "$dir" | tr '/' '_')
        parent_dir=$(dirname "$dir")
        parent_node=$(echo "$parent_dir" | tr '/' '_')
        
        # Add node and connection
        echo "        $node_name[$dir]" >> "$output_file"
        if [ "$parent_dir" != "src" ]; then
            echo "        $parent_node --> $node_name" >> "$output_file"
        fi
    done
    
    echo "    end" >> "$output_file"
}

# Function to verify diagram matches current structure
verify_source_structure() {
    # Generate new diagram in temp file
    generate_source_diagram "$TEMP_DIAGRAM"
    
    # Compare with existing diagram
    if [ ! -f "$SOURCE_DIAGRAM" ]; then
        echo -e "${RED}Error: Source structure diagram does not exist${NC}"
        return 1
    fi
    
    if ! diff "$SOURCE_DIAGRAM" "$TEMP_DIAGRAM" > /dev/null; then
        echo -e "${RED}Error: Source structure diagram is out of date${NC}"
        echo -e "${YELLOW}Expected structure:${NC}"
        cat "$TEMP_DIAGRAM"
        return 1
    fi
    
    echo -e "${GREEN}✓ Source structure diagram is up to date${NC}"
    return 0
}

# Main test execution
main() {
    # Check for --generate flag
    if [ "$1" = "--generate" ]; then
        if [ ! -d "src" ]; then
            echo -e "${RED}Error: src directory not found${NC}"
            exit 1
        fi
        generate_source_diagram
        echo -e "${GREEN}Generated source structure diagram${NC}"
        exit 0
    fi

    echo -e "\n${BLUE}=== Testing Source Structure Diagram ===${NC}"
    
    # Check if src directory exists
    if [ ! -d "src" ]; then
        echo -e "${RED}Error: src directory not found${NC}"
        exit 1
    fi
    
    # Verify current diagram matches structure
    if ! verify_source_structure; then
        echo -e "${RED}Test failed: Source structure diagram needs updating${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Test passed: Source structure diagram is current${NC}"
    exit 0
}

# Run main if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi 