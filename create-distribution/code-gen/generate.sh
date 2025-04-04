#!/bin/bash

# Default values
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_OUTPUT_DIR="$SCRIPT_DIR/generated"
TEMPLATES_DIR="$SCRIPT_DIR/templates"

# Source variables
source "$SCRIPT_DIR/config/variables.sh"

# Parse command line arguments
OUTPUT_DIR="${1:-$DEFAULT_OUTPUT_DIR}"

# Function to create directory structure
create_directory_structure() {
    local base_dir="$1"
    
    # Create main directory structure
    mkdir -p "$base_dir/src/main/java/com/sql/runner/"{commands,model,service,util}
    mkdir -p "$base_dir/src/test/java/com/sql/runner/"{commands,model,service,util}
    
    echo "Created directory structure in $base_dir"
}

# Function to process template
process_template() {
    local template="$1"
    local output="$2"
    
    # Create directory if it doesn't exist
    mkdir -p "$(dirname "$output")"
    
    # Process template with variables
    sed \
        -e "s|{{PACKAGE_NAME}}|${PACKAGE_NAME}|g" \
        -e "s|{{APP_VERSION}}|${APP_VERSION}|g" \
        -e "s|{{APP_NAME}}|${APP_NAME}|g" \
        "$template" > "$output"
        
    echo "Processed template: $template -> $output"
}

# Main execution
echo "Generating project in: $OUTPUT_DIR"

# Create directory structure
create_directory_structure "$OUTPUT_DIR"

# Process main templates
process_template "$TEMPLATES_DIR/pom.xml.template" "$OUTPUT_DIR/pom.xml"
process_template "$TEMPLATES_DIR/App.java.template" "$OUTPUT_DIR/src/main/java/com/sql/runner/SqlRunnerApp.java"
process_template "$TEMPLATES_DIR/model/ConnectionConfig.java.template" "$OUTPUT_DIR/src/main/java/com/sql/runner/model/ConnectionConfig.java"
process_template "$TEMPLATES_DIR/util/OracleConnector.java.template" "$OUTPUT_DIR/src/main/java/com/sql/runner/util/OracleConnector.java"

# Process test templates
process_template "$TEMPLATES_DIR/model/ConnectionConfig.test.java.template" "$OUTPUT_DIR/src/test/java/com/sql/runner/model/ConnectionConfigTest.java"
process_template "$TEMPLATES_DIR/util/OracleConnector.test.java.template" "$OUTPUT_DIR/src/test/java/com/sql/runner/util/OracleConnectorTest.java"

# Make files executable if needed
chmod +x "$OUTPUT_DIR/generate.sh"

echo "Project generation complete!" 