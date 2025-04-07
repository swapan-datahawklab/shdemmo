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

# Build the application only if SKIP_MVN_BUILD is not set
if [ -z "$SKIP_MVN_BUILD" ]; then
    echo "Building application..."
    mvn clean package
else
    echo "Skipping Maven build as SKIP_MVN_BUILD is set"
fi

# Run dependency check to get required modules
echo "Analyzing required Java modules..."
# ./dependency-check.sh
# REQUIRED_MODULES=$(cat dependency-analysis/java-modules.txt | tr '\n' ',' | sed 's/,$//')

REQUIRED_MODULES="java.base,java.logging,java.sql,java.desktop,java.naming,java.management,java.xml,jdk.unsupported,java.security.jgss,java.security.sasl,jdk.crypto.ec,java.transaction.xa"


# Create bundle directory structure
echo "Creating bundle directory structure..."
rm -rf "$BUNDLE_NAME"
mkdir -p "$BUNDLE_NAME/app"
mkdir -p "$BUNDLE_NAME/logs"

# Create custom JRE
echo "Creating custom JRE with modules: $REQUIRED_MODULES"
jlink \
    --add-modules "$REQUIRED_MODULES" \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output "$BUNDLE_NAME/runtime"

# Copy application files
echo "Copying application files..."
cp "target/${APP_NAME}-${APP_VERSION}.jar" "$BUNDLE_NAME/app/"

# Copy Oracle JDBC driver
echo "Copying Oracle JDBC driver..."
# Try to find the Oracle driver in different possible locations
ORACLE_DRIVER_FOUND=false

# Check in target/dependency directory
if [ -f "target/dependency/ojdbc11.jar" ]; then
    echo "Found Oracle driver in target/dependency"
    cp "target/dependency/ojdbc11.jar" "$BUNDLE_NAME/app/"
    ORACLE_DRIVER_FOUND=true
fi

# If not found, try to download it using Maven
if [ "$ORACLE_DRIVER_FOUND" = false ]; then
    echo "Oracle driver not found in target/dependency, attempting to download..."
    mvn dependency:copy -Dartifact=com.oracle.database.jdbc:ojdbc11:23.7.0.25.01 -DoutputDirectory=target/dependency
    if [ -f "target/dependency/ojdbc11.jar" ]; then
        echo "Successfully downloaded Oracle driver"
        cp "target/dependency/ojdbc11.jar" "$BUNDLE_NAME/app/"
        ORACLE_DRIVER_FOUND=true
    fi
fi

# If still not found, try to find it in the local Maven repository
if [ "$ORACLE_DRIVER_FOUND" = false ]; then
    echo "Oracle driver not found in target/dependency, checking local Maven repository..."
    ORACLE_JAR=$(find ~/.m2/repository -name "ojdbc11-*.jar" | head -n 1)
    if [ -n "$ORACLE_JAR" ]; then
        echo "Found Oracle driver in Maven repository: $ORACLE_JAR"
        cp "$ORACLE_JAR" "$BUNDLE_NAME/app/ojdbc11.jar"
        ORACLE_DRIVER_FOUND=true
    fi
fi

if [ "$ORACLE_DRIVER_FOUND" = false ]; then
    echo "Error: Could not find Oracle JDBC driver. Please ensure it is available in your Maven repository."
    exit 1
fi

# Verify the driver was copied
if [ -f "$BUNDLE_NAME/app/ojdbc11.jar" ]; then
    echo "Successfully copied Oracle driver to bundle: $BUNDLE_NAME/app/ojdbc11.jar"
else
    echo "Error: Oracle driver was not copied to bundle"
    exit 1
fi

# Process and copy launcher script
echo "Processing launcher script..."
sed -e "s|target/shdemmo-1.0-SNAPSHOT.jar|app/shdemmo-1.0-SNAPSHOT.jar|g" \
    -e "s|SCRIPT_FILE=\$1|# Handle arguments after --\n    while [ \$# -gt 0 ]; do\n        if [ \"\$1\" = \"--\" ]; then\n            shift\n            break\n        fi\n        shift\n    done\n    # Remaining arguments are for the Java application\n    SCRIPT_FILE=\$1|g" \
    -e "s|exec \"\$JAVA\"|\"\$JAVA\" -Djava.util.logging.config.file=/dev/null -Dlogback.configurationFile=/dev/null -Doracle.jdbc.Trace=false -jar|g" \
    "create-distribution/run.sh.template" > "$BUNDLE_NAME/run.sh"
chmod +x "$BUNDLE_NAME/run.sh"

# Create Windows launcher
echo "Creating Windows launcher..."
cat > "$BUNDLE_NAME/run.bat" << 'EOF'
@echo off
setlocal enabledelayedexpansion

REM Default values
set DB_TYPE=oracle
set DB_HOST=localhost
set DB_PORT=1521
set DB_USER=hr
set DB_PASS=hr
set DB_NAME=freepdb1
set STOP_ON_ERROR=true
set AUTO_COMMIT=false
set PRINT_STATEMENTS=false
set IS_FUNCTION=false
set RETURN_TYPE=NUMERIC
set DRIVER_PATH=
set CSV_OUTPUT=
set SCRIPT_FILE=

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :end_parse
if "%~1"=="-t" (
    set DB_TYPE=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--type" (
    set DB_TYPE=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-H" (
    set DB_HOST=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--host" (
    set DB_HOST=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-P" (
    set DB_PORT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--port" (
    set DB_PORT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-u" (
    set DB_USER=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--username" (
    set DB_USER=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-p" (
    set DB_PASS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--password" (
    set DB_PASS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-d" (
    set DB_NAME=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--database" (
    set DB_NAME=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--stop-on-error" (
    set STOP_ON_ERROR=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--auto-commit" (
    set AUTO_COMMIT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--print-statements" (
    set PRINT_STATEMENTS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--function" (
    set IS_FUNCTION=true
    shift
    goto :parse_args
)
if "%~1"=="--return-type" (
    set RETURN_TYPE=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-i" (
    set INPUT_PARAMS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--input" (
    set INPUT_PARAMS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-o" (
    set OUTPUT_PARAMS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--output" (
    set OUTPUT_PARAMS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--io" (
    set IO_PARAMS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--driver-path" (
    set DRIVER_PATH=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--csv-output" (
    set CSV_OUTPUT=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--" (
    shift
    goto :end_parse
)
if "%~1"=="" goto :end_parse
if not "%~1"=="-*" (
    set SCRIPT_FILE=%~1
    shift
    goto :parse_args
)
echo Error: Unknown option %~1
echo Usage: %~nx0 [database options] ^<script_file^>
echo Database options:
echo   -t, --type ^<type^>        Database type (oracle, sqlserver, postgresql, mysql)
echo   -H, --host ^<host^>        Database host
echo   -P, --port ^<port^>        Database port
echo   -u, --username ^<user^>    Database username
echo   -p, --password ^<pass^>    Database password
echo   -d, --database ^<db^>      Database name
echo   --stop-on-error ^<bool^>   Stop execution on error (default: true)
echo   --auto-commit ^<bool^>     Auto-commit mode (default: false)
echo   --print-statements ^<bool^> Print SQL statements (default: false)
echo   --function              Execute as function
echo   --return-type ^<type^>    Return type for functions (default: NUMERIC)
echo   -i, --input ^<params^>    Input parameters (name:type:value,...)
echo   -o, --output ^<params^>   Output parameters (name:type,...)
echo   --io ^<params^>           Input/Output parameters (name:type:value,...)
echo   --driver-path ^<path^>    Path to JDBC driver JAR file
echo   --csv-output ^<file^>     Output file for CSV format
exit /b 1

:end_parse

REM Check if script file is provided
if "%SCRIPT_FILE%"=="" (
    echo Error: SQL script file or stored procedure name is required
    exit /b 1
)

REM Build the command
set CMD="%~dp0runtime\bin\java" -cp "%~dp0app\ojdbc11.jar;%~dp0app\shdemmo-1.0-SNAPSHOT.jar" -Djava.util.logging.config.file=NUL -Dlogback.configurationFile=NUL -Doracle.jdbc.Trace=false -jar "%~dp0app\shdemmo-1.0-SNAPSHOT.jar"
set CMD=%CMD% -t %DB_TYPE%
set CMD=%CMD% -H %DB_HOST%
set CMD=%CMD% -P %DB_PORT%
set CMD=%CMD% -u %DB_USER%
set CMD=%CMD% -p %DB_PASS%

REM Handle Oracle connection string format
if "%DB_TYPE%"=="oracle" (
    set CMD=%CMD% -d "jdbc:oracle:thin:@%DB_HOST%:%DB_PORT%/%DB_NAME%"
) else (
    set CMD=%CMD% -d %DB_NAME%
)

set CMD=%CMD% --stop-on-error %STOP_ON_ERROR%
set CMD=%CMD% --auto-commit %AUTO_COMMIT%
set CMD=%CMD% --print-statements %PRINT_STATEMENTS%

REM Add optional parameters if provided
if "%IS_FUNCTION%"=="true" (
    set CMD=%CMD% --function
)

if not "%RETURN_TYPE%"=="" (
    set CMD=%CMD% --return-type %RETURN_TYPE%
)

if not "%INPUT_PARAMS%"=="" (
    set CMD=%CMD% -i "%INPUT_PARAMS%"
)

if not "%OUTPUT_PARAMS%"=="" (
    set CMD=%CMD% -o "%OUTPUT_PARAMS%"
)

if not "%IO_PARAMS%"=="" (
    set CMD=%CMD% --io "%IO_PARAMS%"
)

if not "%DRIVER_PATH%"=="" (
    set CMD=%CMD% --driver-path "%DRIVER_PATH%"
)

if not "%CSV_OUTPUT%"=="" (
    set CMD=%CMD% --csv-output "%CSV_OUTPUT%"
)

REM Add the script file or stored procedure name
set CMD=%CMD% "%SCRIPT_FILE%"

REM Execute the command
%CMD%
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