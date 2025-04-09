#!/bin/bash

# Exit on error
set -e

# Configuration
APP_NAME="shdemmo"
APP_VERSION="1.0-SNAPSHOT"
TEMPLATE_DIR="create-distribution/templates"
OUTPUT_DIR="target/generated-sources/launchers"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Function to convert Java type to shell type
convert_type() {
    case "$1" in
        "boolean") echo "bool" ;;
        "String") echo "string" ;;
        "int"|"Integer") echo "number" ;;
        *) echo "string" ;;
    esac
}

# Function to generate shell script
generate_shell_script() {
    local options_file="$1"
    local template_file="$2"
    local output_file="$3"

    # Read options from file
    while IFS= read -r line; do
        if [[ $line =~ ^([^:]+):([^:]+):([^:]+):([^:]+)$ ]]; then
            local name="${BASH_REMATCH[1]}"
            local type="${BASH_REMATCH[2]}"
            local short_opt="${BASH_REMATCH[3]}"
            local long_opt="${BASH_REMATCH[4]}"
            local shell_type=$(convert_type "$type")

            # Add to usage
            if [ -n "$short_opt" ]; then
                echo "  $short_opt, $long_opt <$shell_type>    $name"
            else
                echo "  $long_opt <$shell_type>    $name"
            fi

            # Add to case statement
            if [ -n "$short_opt" ]; then
                echo "        $short_opt|$long_opt)"
                echo "            ${name^^}=\"$2\""
                echo "            shift 2"
                echo "            ;;"
            else
                echo "        $long_opt)"
                echo "            ${name^^}=\"$2\""
                echo "            shift 2"
                echo "            ;;"
            fi

            # Add to command building
            echo "if [ -n \"\$${name^^}\" ]; then"
            echo "    CMD+=(\"$long_opt\" \"\$${name^^}\")"
            echo "fi"
        fi
    done < "$options_file" > "$output_file"
}

# Function to generate batch script
generate_batch_script() {
    local options_file="$1"
    local template_file="$2"
    local output_file="$3"

    # Read options from file
    while IFS= read -r line; do
        if [[ $line =~ ^([^:]+):([^:]+):([^:]+):([^:]+)$ ]]; then
            local name="${BASH_REMATCH[1]}"
            local type="${BASH_REMATCH[2]}"
            local short_opt="${BASH_REMATCH[3]}"
            local long_opt="${BASH_REMATCH[4]}"
            local shell_type=$(convert_type "$type")

            # Add to usage
            if [ -n "$short_opt" ]; then
                echo "echo   $short_opt, $long_opt ^<$shell_type^>    $name"
            else
                echo "echo   $long_opt ^<$shell_type^>    $name"
            fi

            # Add to case statement
            if [ -n "$short_opt" ]; then
                echo "if \"%~1\"==\"$short_opt\" ("
                echo "    set $name=%~2"
                echo "    shift"
                echo "    shift"
                echo "    goto :parse_args"
                echo ")"
            fi
            echo "if \"%~1\"==\"$long_opt\" ("
            echo "    set $name=%~2"
            echo "    shift"
            echo "    shift"
            echo "    goto :parse_args"
            echo ")"
        fi
    done < "$options_file" > "$output_file"
}

# Extract Picocli options from Java code
echo "Extracting Picocli options..."
mvn compile
java -cp target/classes com.example.shelldemo.App --generate-options > "$OUTPUT_DIR/options.txt"

# Generate shell script
echo "Generating shell script..."
generate_shell_script "$OUTPUT_DIR/options.txt" "$TEMPLATE_DIR/run.sh.template" "$OUTPUT_DIR/run.sh"

# Generate batch script
echo "Generating batch script..."
generate_batch_script "$OUTPUT_DIR/options.txt" "$TEMPLATE_DIR/run.bat.template" "$OUTPUT_DIR/run.bat"

# Copy generated scripts to template directory
cp "$OUTPUT_DIR/run.sh" "$TEMPLATE_DIR/run.sh.template"
cp "$OUTPUT_DIR/run.bat" "$TEMPLATE_DIR/run.bat.template"

echo "Launcher scripts generated successfully!" 