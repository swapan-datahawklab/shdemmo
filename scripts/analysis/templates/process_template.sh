#!/bin/bash

# Template Processing Script
# Usage: ./process_template.sh <template_name> <output_file> [variables]

set -e  # Exit on error

# Constants
TEMPLATE_DIR=".cursor/rules/templates/memory-bank"
DEFAULT_FRONT_MATTER="---\ndescription: \"$2\"\n---\n"

# Functions
check_template() {
    local template="$TEMPLATE_DIR/$1"
    if [[ ! -f "$template" ]]; then
        echo "Error: Template $template not found"
        exit 1
    fi
}

create_file() {
    local output="$1"
    local content="$2"
    
    # Create directory if it doesn't exist
    mkdir -p "$(dirname "$output")"
    
    # Write content
    echo -e "$content" > "$output"
}

process_template() {
    local template="$TEMPLATE_DIR/$1"
    local output="$2"
    
    # Read template content (skip front matter)
    local content=$(sed -n '/^---$/,/^---$/!p' "$template")
    
    # Remove template markers
    content=$(echo "$content" | sed '/# Your rule content/d')
    content=$(echo "$content" | sed '/You can @ files here/d')
    content=$(echo "$content" | sed '/You can use markdown but dont have to/d')
    
    # Add proper front matter
    echo -e "${DEFAULT_FRONT_MATTER}\n${content}"
}

# Main
if [[ $# -lt 2 ]]; then
    echo "Usage: $0 <template_name> <output_file>"
    exit 1
fi

TEMPLATE_NAME="$1"
OUTPUT_FILE="$2"

# Validate template exists
check_template "$TEMPLATE_NAME"

# Process template and create file
PROCESSED_CONTENT=$(process_template "$TEMPLATE_NAME" "$OUTPUT_FILE")
create_file "$OUTPUT_FILE" "$PROCESSED_CONTENT"

echo "Created $OUTPUT_FILE from template $TEMPLATE_NAME" 