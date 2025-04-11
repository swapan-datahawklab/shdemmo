#!/bin/bash

# Exit on error
set -e

# Ensure consistent behavior across platforms
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL="*"

# Detect OS and shell environment
detect_environment() {
    case "$(uname -s)" in
        Linux*)     
            IS_WINDOWS=false
            BUNDLE_NAME="shdemmo-bundle-linux"
            ;;
        MINGW*|MSYS*|CYGWIN*)     
            IS_WINDOWS=true
            BUNDLE_NAME="shdemmo-bundle-windows"
            ;;
        *)          
            log_error "Unsupported operating system"
            exit 1
            ;;
    esac
}

# Help function
show_help() {
    cat << EOF
Usage: $(basename "$0") [options]

Options:
    -h, --help              Show this help message
    -a, --add-drivers      Download common JDBC drivers from Maven (Oracle, MySQL, PostgreSQL, SQL Server)

The --add-drivers flag will download JDBC drivers specified in drivers.properties and include them in the bundle.
Drivers will be organized by type in the bundle's 'drivers' directory.

Example:
    ./$(basename "$0") --add-drivers    # Create bundle with all common JDBC drivers
    ./$(basename "$0")                  # Create bundle without drivers
EOF
    exit 0
}

# Get absolute path in a cross-platform way
get_absolute_path() {
    local path="$1"
    if [ "$IS_WINDOWS" = true ]; then
        echo "$(cd "$(dirname "$path")" && pwd -W)/$(basename "$path")"
    else
        echo "$(cd "$(dirname "$path")" && pwd)/$(basename "$path")"
    fi
}

# Find Maven repository in a cross-platform way
find_maven_home() {
    if [ "$IS_WINDOWS" = true ]; then
        echo "$USERPROFILE/.m2"
    else
        echo "$HOME/.m2"
    fi
}

# Parse command line arguments
ADD_COMMON_DRIVERS=false
while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help)
            show_help
            ;;
        -a|--add-drivers)
            ADD_COMMON_DRIVERS=true
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            ;;
    esac
done

# Detect environment
detect_environment

# Configuration
APP_NAME="shdemmo"
APP_VERSION="1.0-SNAPSHOT"
MAIN_CLASS="com.example.shelldemo.App"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
M2_HOME="$(find_maven_home)"

# Colors for output
if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    NC='\033[0m' # No Color
else
    RED=''
    GREEN=''
    YELLOW=''
    NC=''
fi

# Cross-platform compatible file operations
copy_file() {
    local src="$1"
    local dest="$2"
    if [ "$IS_WINDOWS" = true ]; then
        cp "$(cygpath -w "$src")" "$(cygpath -w "$dest")"
    else
        cp "$src" "$dest"
    fi
}

make_executable() {
    local file="$1"
    if [ "$IS_WINDOWS" = false ]; then
        chmod +x "$file"
    fi
}

# Function to download JDBC driver from Maven
download_jdbc_driver() {
    local db_type="$1"
    local maven_coords="$2"
    local target_dir="$3"
    
    log_info "Downloading $db_type JDBC driver..."
    
    # Create target directory if it doesn't exist
    mkdir -p "$target_dir"
    
    # Split Maven coordinates
    IFS=':' read -r groupId artifactId version <<< "$maven_coords"
    
    # Download using Maven with explicit repository path
    mvn dependency:copy \
        -Dartifact="$maven_coords" \
        -DoutputDirectory="$target_dir" \
        -Dmdep.stripVersion=true \
        -Dmaven.repo.local="$M2_HOME/repository" \
        || {
            log_error "Failed to download $db_type driver"
            return 1
        }
    
    # Rename the jar to remove version
    local jar_name="$artifactId.jar"
    if [ -f "$target_dir/$artifactId-$version.jar" ]; then
        mv "$target_dir/$artifactId-$version.jar" "$target_dir/$jar_name"
    fi
    
    log_info "Successfully downloaded $db_type driver to $target_dir/$jar_name"
}

# Function to download all common drivers
download_common_drivers() {
    local bundle_dir="$1"
    local drivers_file="$SCRIPT_DIR/drivers.properties"
    
    if [ ! -f "$drivers_file" ]; then
        log_error "drivers.properties not found at $drivers_file"
        return 1
    }
    
    # Read and process drivers.properties
    while IFS='=' read -r key value || [ -n "$key" ]; do
        # Skip comments and empty lines
        [[ $key =~ ^#.*$ ]] && continue
        [ -z "$key" ] && continue
        
        # Extract database type from key
        local db_type="${key%%.driver}"
        local target_dir="$bundle_dir/drivers/$db_type"
        
        # Download driver
        download_jdbc_driver "$db_type" "$value" "$target_dir"
    done < "$drivers_file"
}

# Create the bundle directory structure
create_bundle_structure() {
    local BUNDLE_DIR="$1"
    
    # Create directories
    mkdir -p "$BUNDLE_DIR"/{app,runtime,drivers/{oracle,mysql,postgresql,sqlserver}}
    
    # Copy templates with path conversion
    copy_file "$SCRIPT_DIR/templates/run.sh.template" "$BUNDLE_DIR/run.sh"
    copy_file "$SCRIPT_DIR/templates/run.bat.template" "$BUNDLE_DIR/run.bat"
    
    # Make run scripts executable
    make_executable "$BUNDLE_DIR/run.sh"
    
    log_info "Created bundle structure in $BUNDLE_DIR"
}

# Main execution starts here
log_info "Starting bundle creation on $(uname -s)"

# Ensure we're in the project root directory
if [ ! -f "pom.xml" ]; then
    log_error "Please run this script from the project root directory"
    exit 1
fi

# Build the application only if SKIP_MVN_BUILD is not set
if [ -z "$SKIP_MVN_BUILD" ]; then
    log_info "Building application..."
    mvn clean package
else
    log_info "Skipping Maven build as SKIP_MVN_BUILD is set"
fi

# Run dependency check to get required modules
echo "Analyzing required Java modules..."
# ./dependency-check.sh
# REQUIRED_MODULES=$(cat dependency-analysis/java-modules.txt | tr '\n' ',' | sed 's/,$//')

REQUIRED_MODULES="java.base,java.logging,java.sql,java.desktop,java.naming,java.management,java.xml,jdk.unsupported,java.security.jgss,java.security.sasl,jdk.crypto.ec,java.transaction.xa"

# Create bundle directory structure
log_info "Creating bundle directory structure..."
rm -rf "$BUNDLE_NAME"
create_bundle_structure "$BUNDLE_NAME"

# Download common drivers if requested
if [ "$ADD_COMMON_DRIVERS" = true ]; then
    log_info "Downloading common JDBC drivers..."
    download_common_drivers "$BUNDLE_NAME"
fi

# Create custom JRE (only for Linux)
if [ "$IS_WINDOWS" = false ]; then
    log_info "Creating custom JRE with modules: $REQUIRED_MODULES"
    jlink \
        --add-modules "$REQUIRED_MODULES" \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=2 \
        --output "$BUNDLE_NAME/runtime"
fi

# Copy application files
log_info "Copying application files..."
cp "target/${APP_NAME}-${APP_VERSION}.jar" "$BUNDLE_NAME/app/"

# Copy Oracle JDBC driver
log_info "Copying Oracle JDBC driver..."
# Try to find the Oracle driver in different possible locations
ORACLE_DRIVER_FOUND=false

# Check in target/dependency directory
if [ -f "target/dependency/ojdbc11.jar" ]; then
    log_info "Found Oracle driver in target/dependency"
    cp "target/dependency/ojdbc11.jar" "$BUNDLE_NAME/app/"
    ORACLE_DRIVER_FOUND=true
fi

# If not found, try to download it using Maven
if [ "$ORACLE_DRIVER_FOUND" = false ]; then
    log_info "Oracle driver not found in target/dependency, attempting to download..."
    mvn dependency:copy -Dartifact=com.oracle.database.jdbc:ojdbc11:23.7.0.25.01 -DoutputDirectory=target/dependency
    if [ -f "target/dependency/ojdbc11.jar" ]; then
        log_info "Successfully downloaded Oracle driver"
        cp "target/dependency/ojdbc11.jar" "$BUNDLE_NAME/app/"
        ORACLE_DRIVER_FOUND=true
    fi
fi

# If still not found, try to find it in the local Maven repository
if [ "$ORACLE_DRIVER_FOUND" = false ]; then
    log_info "Oracle driver not found in target/dependency, checking local Maven repository..."
    ORACLE_JAR=$(find ~/.m2/repository -name "ojdbc11-*.jar" | head -n 1)
    if [ -n "$ORACLE_JAR" ]; then
        log_info "Found Oracle driver in Maven repository: $ORACLE_JAR"
        cp "$ORACLE_JAR" "$BUNDLE_NAME/app/ojdbc11.jar"
        ORACLE_DRIVER_FOUND=true
    fi
fi

if [ "$ORACLE_DRIVER_FOUND" = false ]; then
    log_error "Error: Could not find Oracle JDBC driver. Please ensure it is available in your Maven repository."
    exit 1
fi

# Verify the driver was copied
if [ -f "$BUNDLE_NAME/app/ojdbc11.jar" ]; then
    log_info "Successfully copied Oracle driver to bundle: $BUNDLE_NAME/app/ojdbc11.jar"
else
    log_error "Error: Oracle driver was not copied to bundle"
    exit 1
fi

# Process and copy launcher script
if [ "$IS_WINDOWS" = true ]; then
    log_info "Creating Windows launcher..."
    cp "create-distribution/run.bat.template" "$BUNDLE_NAME/run.bat"
else
    log_info "Creating Linux launcher..."
    sed -e "s|target/shdemmo-1.0-SNAPSHOT.jar|app/shdemmo-1.0-SNAPSHOT.jar|g" \
        -e "s|SCRIPT_FILE=\$1|# Handle arguments after --\n    while [ \$# -gt 0 ]; do\n        if [ \"\$1\" = \"--\" ]; then\n            shift\n            break\n        fi\n        shift\n    done\n    # Remaining arguments are for the Java application\n    SCRIPT_FILE=\$1|g" \
        -e "s|exec \"\$JAVA\"|\"\$JAVA\" -Djava.util.logging.config.file=/dev/null -Dlogback.configurationFile=/dev/null -Doracle.jdbc.Trace=false -jar|g" \
        "create-distribution/run.sh.template" > "$BUNDLE_NAME/run.sh"
    chmod +x "$BUNDLE_NAME/run.sh"
fi

# Create README for the bundle
echo "Creating bundle README..."
sed -e "s/\${APPLICATION_NAME}/${APP_NAME}/g" \
    -e "s/\${BUNDLE_NAME}/${BUNDLE_NAME}/g" \
    "create-distribution/README.md.template" > "$BUNDLE_NAME/README.md"

# Create archive for Windows
if [ "$IS_WINDOWS" = true ]; then
    log_info "Creating Windows bundle archive..."
    if command -v 7z >/dev/null 2>&1; then
        7z a -tzip "${BUNDLE_NAME}.zip" "$BUNDLE_NAME"
    else
        log_warn "7z not found, using zip instead"
        zip -r "${BUNDLE_NAME}.zip" "$BUNDLE_NAME"
    fi
    log_info "Windows bundle created: ${BUNDLE_NAME}.zip"
else
    log_info "Linux bundle created in directory: $BUNDLE_NAME"
fi

log_info "Bundle creation completed successfully!"
echo "You can find the bundle in: ${BUNDLE_NAME}.zip"
echo "To use the application:"
echo "1. Extract the archive: unzip ${BUNDLE_NAME}.zip"
echo "2. Run the application:"
echo "   - On Linux/macOS: ./${BUNDLE_NAME}/run.sh"
echo "   - On Windows: ${BUNDLE_NAME}\\run.bat" 