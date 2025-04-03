#!/bin/bash

# Exit on any error
set -e

# Configuration
APP_NAME="shdemmo"
APP_VERSION="1.0-SNAPSHOT"
BUNDLE_NAME="${APP_NAME}-bundle-windows"
MAIN_CLASS="com.example.shelldemo.App"

# Convert JAVA_HOME to Unix-style path if it exists
if [ -n "$JAVA_HOME" ]; then
    JAVA_HOME=$(cygpath -u "$JAVA_HOME")
fi

# Ensure we're in the project root directory
if [ ! -f "pom.xml" ]; then
    echo "Error: Please run this script from the project root directory"
    exit 1
fi

# Build the application
echo "Building application..."
./mvnw.cmd clean package

# Create bundle directory structure
echo "Creating bundle directory structure..."
rm -rf "$BUNDLE_NAME"
mkdir -p "$BUNDLE_NAME/app"
mkdir -p "$BUNDLE_NAME/logs"

# Create custom JRE for Windows
echo "Creating custom JRE..."
if [ -z "$JAVA_HOME" ]; then
    echo "Error: JAVA_HOME environment variable is not set"
    exit 1
fi

"$JAVA_HOME/bin/jlink" \
    --add-modules java.base,java.logging,java.xml,java.sql,java.desktop,java.management,java.naming,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output "$BUNDLE_NAME/runtime"

# Copy application files
echo "Copying application files..."
cp "target/${APP_NAME}-${APP_VERSION}.jar" "$BUNDLE_NAME/app/"

# Process and copy logback configuration
echo "Copying logging configuration..."
sed -e "s/\${LOG_DIRECTORY:-logs}/logs/g" \
    -e "s/\${LOG_FILENAME:-application.log}/application.log/g" \
    -e "s/\${LOG_LEVEL:-INFO}/INFO/g" \
    -e "s/\${ROOT_LOG_LEVEL:-WARN}/WARN/g" \
    "create-distribution/logback.xml.template" > "$BUNDLE_NAME/app/logback.xml"

# Create Windows launcher (as .bat)
echo "Creating Windows launcher..."
cat > "$BUNDLE_NAME/run.bat" << 'EOF'
@echo off
setlocal

REM Set logging mode from command line argument
set LOG_MODE=default
if not "%~1"=="" (
    if "%~1"=="-l" (
        set LOG_MODE=%~2
        shift
        shift
    )
)

REM Set the classpath to include the app directory for logback config
set "APP_DIR=%~dp0app"
set "CLASSPATH=%APP_DIR%\*;%APP_DIR%"

REM Run the application with the custom runtime
"%~dp0runtime\bin\java" ^
    -Dlogback.configurationFile="%APP_DIR%\logback.xml" ^
    -Dapp.level=INFO ^
    -Droot.level=WARN ^
    -cp "%CLASSPATH%" ^
    com.example.shelldemo.App %*

endlocal
EOF

# Create a zip archive (more common on Windows)
echo "Creating archive..."
if command -v zip >/dev/null 2>&1; then
    zip -r "${BUNDLE_NAME}.zip" "$BUNDLE_NAME"
else
    echo "Warning: zip command not found, falling back to tar.gz"
    tar -czf "${BUNDLE_NAME}.tar.gz" "$BUNDLE_NAME"
fi

echo "Bundle created successfully!"
if [ -f "${BUNDLE_NAME}.zip" ]; then
    echo "You can find the bundle in: ${BUNDLE_NAME}.zip"
else
    echo "You can find the bundle in: ${BUNDLE_NAME}.tar.gz"
fi
echo "To use the application:"
echo "1. Extract the archive"
echo "2. Run the application: ${BUNDLE_NAME}\\run.bat" 