#Requires -Version 5.0

<#
.SYNOPSIS
    Creates a Windows bundle for the shdemmo application.

.DESCRIPTION
    This script builds the application using Maven and creates a distributable Windows bundle
    containing a custom JRE, the application JAR, and necessary configuration files.

.NOTES
    If you get an execution policy error, you have several options:

    1. Run with bypass (recommended for one-time use):
       powershell -ExecutionPolicy Bypass -File .\create-distribution\create-bundle-windows.ps1

    2. Change execution policy for current session only:
       Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
       .\create-distribution\create-bundle-windows.ps1

    3. Change execution policy permanently (requires admin):
       Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

.EXAMPLE
    .\create-bundle-windows.ps1
    
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\create-distribution\create-bundle-windows.ps1
#>

# Constants
$APP_NAME = "shdemmo"
$APP_VERSION = "1.0-SNAPSHOT"
$BUNDLE_NAME = "$APP_NAME-bundle-windows"
$MAIN_CLASS = "com.example.shelldemo.App"
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

# Colors for PowerShell output
$Colors = @{
    Red    = 'Red'
    Green  = 'Green'
    Yellow = 'Yellow'
    White  = 'White'
}

# Logging functions
function Write-LogInfo {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Colors.Green
}

function Write-LogWarn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor $Colors.Yellow
}

function Write-LogError {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Colors.Red
}

# Function to check if we're in the project root
function Test-ProjectRoot {
    if (-not (Test-Path "pom.xml")) {
        Write-LogError "Error: Please run this script from the project root directory"
        return $false
    }
    return $true
}

# Function to build the application with Maven
function Build-Application {
    Write-LogInfo "Building application with Maven..."
    
    try {
        mvn clean package
        if ($LASTEXITCODE -ne 0) {
            Write-LogError "Maven build failed"
            return $false
        }
    }
    catch {
        Write-LogError "Exception during build: $_"
        return $false
    }
    
    Write-LogInfo "Application built successfully"
    return $true
}

# Function to create a custom JRE
function New-CustomJRE {
    Write-LogInfo "Creating custom JRE..."
    
    try {
        $jlinkModules = @(
            "java.base",
            "java.logging",
            "java.xml",
            "java.sql",
            "java.desktop",
            "java.management",
            "java.naming",
            "jdk.unsupported"
        )

        $jlinkArgs = @(
            "--add-modules", ($jlinkModules -join ","),
            "--strip-debug",
            "--no-man-pages",
            "--no-header-files",
            "--compress=zip-6",
            "--output", "$BUNDLE_NAME/runtime"
        )

        jlink $jlinkArgs
        if ($LASTEXITCODE -ne 0) {
            Write-LogError "Failed to create custom JRE"
            return $false
        }
    }
    catch {
        Write-LogError "Exception during JRE creation: $_"
        return $false
    }
    
    Write-LogInfo "Custom JRE created successfully"
    return $true
}

# Function to create the bundle directory structure
function New-BundleStructure {
    Write-LogInfo "Creating bundle directory structure..."
    
    try {
        # Remove existing bundle if it exists
        if (Test-Path $BUNDLE_NAME) {
            Remove-Item -Path $BUNDLE_NAME -Recurse -Force
        }
        
        # Create bundle directories
        New-Item -ItemType Directory -Path "$BUNDLE_NAME/app" -Force | Out-Null
        New-Item -ItemType Directory -Path "$BUNDLE_NAME/logs" -Force | Out-Null
        
        Write-LogInfo "Bundle structure created successfully"
        return $true
    }
    catch {
        Write-LogError "Failed to create bundle structure: $_"
        return $false
    }
}

# Function to copy application files
function Copy-ApplicationFiles {
    Write-LogInfo "Copying application files..."
    
    try {
        # Copy JAR file
        Copy-Item "target/$APP_NAME-$APP_VERSION.jar" "$BUNDLE_NAME/app/"
        
        # Process and copy logback configuration
        $logbackTemplate = Get-Content "$SCRIPT_DIR/logback.xml.template" -Raw
        $logbackContent = $logbackTemplate -replace '\${LOG_DIRECTORY:-logs}', 'logs' `
                                        -replace '\${LOG_FILENAME:-application.log}', 'application.log' `
                                        -replace '\${LOG_LEVEL:-INFO}', 'INFO' `
                                        -replace '\${ROOT_LOG_LEVEL:-WARN}', 'WARN'
        Set-Content -Path "$BUNDLE_NAME/app/logback.xml" -Value $logbackContent
        
        Write-LogInfo "Application files copied successfully"
        return $true
    }
    catch {
        Write-LogError "Failed to copy application files: $_"
        return $false
    }
}

# Function to create the Windows batch file
function New-WindowsBatchFile {
    Write-LogInfo "Creating Windows batch file..."
    
    try {
        $batchContent = @'
@echo off
setlocal EnableDelayedExpansion

set "JAVA_OPTS="
set "APP_ARGS="
set "LOG_MODE=default"

:parse_args
if "%~1"=="" goto run
if "%~1"=="-l" (
    set "LOG_MODE=%~2"
    shift
    shift
    goto parse_args
)
if "%~1"=="--log-mode" (
    set "LOG_MODE=%~2"
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
if "%LOG_MODE%"=="debug" (
    set "JAVA_OPTS=-Dlogging.level.root=DEBUG"
) else if "%LOG_MODE%"=="trace" (
    set "JAVA_OPTS=-Dlogging.level.root=TRACE"
) else if "%LOG_MODE%"=="quiet" (
    set "JAVA_OPTS=-Dlogging.level.root=WARN"
)

set "SCRIPT_DIR=%~dp0"
set "JAVA_OPTS=%JAVA_OPTS% -Dapp.log.dir=%SCRIPT_DIR%logs -Dlogging.config=%SCRIPT_DIR%app\logback.xml"

"%SCRIPT_DIR%runtime\bin\java" %JAVA_OPTS% -jar "%SCRIPT_DIR%app\*.jar" %APP_ARGS%
exit /b %ERRORLEVEL%
'@

        Set-Content -Path "$BUNDLE_NAME/run.bat" -Value $batchContent
        
        Write-LogInfo "Windows batch file created successfully"
        return $true
    }
    catch {
        Write-LogError "Failed to create Windows batch file: $_"
        return $false
    }
}

# Function to create the README file
function New-ReadmeFile {
    Write-LogInfo "Creating README file..."
    
    try {
        $readmeTemplate = Get-Content "$SCRIPT_DIR/README.md.template" -Raw
        $readmeContent = $readmeTemplate -replace '\${APPLICATION_NAME}', $APP_NAME `
                                       -replace '\${BUNDLE_NAME}', $BUNDLE_NAME
        Set-Content -Path "$BUNDLE_NAME/README.md" -Value $readmeContent
        
        Write-LogInfo "README file created successfully"
        return $true
    }
    catch {
        Write-LogError "Failed to create README file: $_"
        return $false
    }
}

# Function to create the bundle archive
function New-BundleArchive {
    Write-LogInfo "Creating bundle archive..."
    
    try {
        Compress-Archive -Path $BUNDLE_NAME -DestinationPath "$BUNDLE_NAME.zip" -Force
        Write-LogInfo "Bundle archive created successfully: $BUNDLE_NAME.zip"
        return $true
    }
    catch {
        Write-LogError "Failed to create bundle archive: $_"
        return $false
    }
}

# Cleanup function
function Invoke-Cleanup {
    if (Test-Path $BUNDLE_NAME) {
        Write-LogInfo "Cleaning up bundle directory..."
        Remove-Item -Path $BUNDLE_NAME -Recurse -Force
    }
}

# Main function
function Main {
    Write-LogInfo "Starting bundle creation for Windows..."
    
    if (-not (Test-ProjectRoot)) {
        return 1
    }
    
    try {
        $steps = @(
            @{ Name = "Build Application"; Function = { Build-Application } },
            @{ Name = "Create Bundle Structure"; Function = { New-BundleStructure } },
            @{ Name = "Create Custom JRE"; Function = { New-CustomJRE } },
            @{ Name = "Copy Application Files"; Function = { Copy-ApplicationFiles } },
            @{ Name = "Create Windows Batch File"; Function = { New-WindowsBatchFile } },
            @{ Name = "Create README File"; Function = { New-ReadmeFile } },
            @{ Name = "Create Bundle Archive"; Function = { New-BundleArchive } }
        )

        foreach ($step in $steps) {
            if (-not (& $step.Function)) {
                throw "Failed at step: $($step.Name)"
            }
        }
        
        Write-LogInfo "Bundle creation completed successfully"
        Write-LogInfo "You can find the bundle in: $BUNDLE_NAME.zip"
        Write-LogInfo "To use the application:"
        Write-LogInfo "1. Extract the archive"
        Write-LogInfo "2. Run the application: $BUNDLE_NAME\run.bat"
        return 0
    }
    catch {
        Write-LogError "Bundle creation failed: $_"
        return 1
    }
    finally {
        Invoke-Cleanup
    }
}

# Check if running with appropriate execution policy
$currentPolicy = Get-ExecutionPolicy
if ($currentPolicy -eq 'Restricted') {
    Write-LogError @"
Script execution is restricted. To run this script, use one of these options:

1. Run with bypass (recommended for one-time use):
   powershell -ExecutionPolicy Bypass -File $($MyInvocation.MyCommand.Path)

2. Change execution policy for current session only:
   Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
   $($MyInvocation.MyCommand.Path)

3. Change execution policy permanently (requires admin):
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
"@
    exit 1
}

# Run main function and exit with its status code
try {
    $result = Main
    exit $result
}
catch {
    Write-LogError "Unhandled exception: $_"
    exit 1
} 