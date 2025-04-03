#!/bin/bash

# Exit on any error
set -e

# Configuration
APP_NAME="shdemmo"
APP_VERSION="1.0-SNAPSHOT"
BUNDLE_NAME="${APP_NAME}-bundle-linux"
MAIN_CLASS="com.example.shelldemo.App"

# Ensure we're in the project root directory
if [ ! -f "pom.xml" ]; then
    echo "Error: Please run this script from the project root directory"
    exit 1
fi

# Build the application
echo "Building application..."
mvn clean package

# Create bundle directory structure
echo "Creating bundle directory structure..."
rm -rf "$BUNDLE_NAME"
mkdir -p "$BUNDLE_NAME/app"
mkdir -p "$BUNDLE_NAME/logs"

# Create custom JRE
echo "Creating custom JRE..."
jlink \
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

# Copy and configure launcher script
echo "Copying launcher script..."
cp "create-distribution/run.sh.template" "$BUNDLE_NAME/run.sh"
chmod +x "$BUNDLE_NAME/run.sh"

# Create Windows launcher
echo "Creating Windows launcher..."
cat > "$BUNDLE_NAME/run.bat" << 'EOF'
@echo off
"%~dp0runtime\bin\java" -jar "%~dp0app\\"*.jar %*
EOF

# Create README for the bundle
echo "Creating bundle README..."
sed -e "s/\${APPLICATION_NAME}/${APP_NAME}/g" \
    -e "s/\${BUNDLE_NAME}/${BUNDLE_NAME}/g" \
    "create-distribution/README.md.template" > "$BUNDLE_NAME/README.md"

# Create archive
echo "Creating archive..."
tar -czf "${BUNDLE_NAME}.tar.gz" "$BUNDLE_NAME"

echo "Bundle created successfully!"
echo "You can find the bundle in: ${BUNDLE_NAME}.tar.gz"
echo "To use the application:"
echo "1. Extract the archive: tar xzf ${BUNDLE_NAME}.tar.gz"
echo "2. Run the application:"
echo "   - On Linux/macOS: ./${BUNDLE_NAME}/run.sh"
echo "   - On Windows: ${BUNDLE_NAME}\\run.bat" 