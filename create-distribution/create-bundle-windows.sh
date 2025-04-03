#!/bin/bash

# Exit on error, undefined variables, and pipe failures
set -euo pipefail

# Constants
readonly SUCCESS=0
readonly ERROR=1
readonly BUNDLE_NAME="shdemmo-bundle-windows"

# Colors for Git Bash on Windows
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# Function to build the application with Maven
build_application() {
    log_info "Building application..."
    
    if ! mvn clean package -DskipTests; then
        log_error "Failed to build application"
        return $ERROR
    fi
    
    log_info "Application built successfully"
    return $SUCCESS
}

# Function to create the bundle directory structure
create_bundle_structure() {
    local bundle_dir="$BUNDLE_NAME"
    log_info "Creating bundle structure in $bundle_dir"
    
    # Remove existing bundle if it exists
    if [[ -d "$bundle_dir" ]]; then
        rm -rf "$bundle_dir"
    fi
    
    # Create bundle directory
    mkdir -p "$bundle_dir"
    
    # Copy JAR file
    local jar_file
    jar_file=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar")
    if [[ ! -f "$jar_file" ]]; then
        log_error "JAR file not found in target directory"
        return $ERROR
    fi
    
    cp "$jar_file" "$bundle_dir/app.jar"
    
    # Create Windows batch file for running the application
    cat > "$bundle_dir/run.bat" << 'EOF'
@echo off
setlocal EnableDelayedExpansion

set "JAVA_OPTS="
set "APP_ARGS="

:parse_args
if "%~1"=="" goto run
if "%~1"=="-l" (
    set "JAVA_OPTS=!JAVA_OPTS! -Dlogging.level.root=%~2"
    shift
    shift
    goto parse_args
)
if "%~1"=="--" (
    shift
    :collect_app_args
    if "%~1"=="" goto run
    set "APP_ARGS=!APP_ARGS! %~1"
    shift
    goto collect_app_args
)
set "APP_ARGS=!APP_ARGS! %~1"
shift
goto parse_args

:run
java %JAVA_OPTS% -jar app.jar%APP_ARGS%
exit /b %ERRORLEVEL%
EOF
    
    # Make the batch file executable
    chmod +x "$bundle_dir/run.bat"
    
    log_info "Bundle structure created successfully"
    return $SUCCESS
}

# Function to create the final bundle archive
create_bundle_archive() {
    log_info "Creating bundle archive..."
    
    # Create zip archive for Windows
    if ! zip -r "${BUNDLE_NAME}.zip" "$BUNDLE_NAME"; then
        log_error "Failed to create zip archive"
        return $ERROR
    fi
    
    log_info "Bundle archive created successfully: ${BUNDLE_NAME}.zip"
    return $SUCCESS
}

# Cleanup function
cleanup() {
    local exit_code=$?
    if [[ -d "$BUNDLE_NAME" ]]; then
        log_info "Cleaning up bundle directory..."
        rm -rf "$BUNDLE_NAME"
    fi
    exit $exit_code
}

# Register cleanup function
trap cleanup EXIT

# Main function
main() {
    log_info "Starting bundle creation for Windows..."
    
    if ! build_application; then
        return $ERROR
    fi
    
    if ! create_bundle_structure; then
        return $ERROR
    fi
    
    if ! create_bundle_archive; then
        return $ERROR
    fi
    
    log_info "Bundle creation completed successfully"
    return $SUCCESS
}

# Run main function
main "$@" 