#!/bin/bash

# Exit on any error
set -e

# Detect operating system
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    IS_WINDOWS=true
    BUNDLE_NAME="shdemmo-bundle-windows"
else
    IS_WINDOWS=false
    BUNDLE_NAME="shdemmo-bundle-linux"
fi

# Configuration
APP_NAME="shdemmo"
APP_VERSION="1.0-SNAPSHOT"
MAIN_CLASS="com.example.shelldemo.App"

# Colors for output (works in both Git Bash and Linux)
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
mkdir -p "$BUNDLE_NAME/app"
mkdir -p "$BUNDLE_NAME/logs"

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