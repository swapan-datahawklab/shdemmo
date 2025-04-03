#Requires -Version 5.0

<#
.SYNOPSIS
    Creates a Windows bundle for the shdemmo application.

.DESCRIPTION
    This script builds the application using Maven and creates a distributable Windows bundle.

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
$BUNDLE_NAME = "shdemmo-bundle-windows"
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

# Function to build the application with Maven
function Build-Application {
    Write-LogInfo "Building application..."
    
    try {
        mvn clean package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-LogError "Failed to build application"
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

# Function to create the bundle directory structure
function New-BundleStructure {
    Write-LogInfo "Creating bundle structure in $BUNDLE_NAME"
    
    try {
        # Remove existing bundle if it exists
        if (Test-Path $BUNDLE_NAME) {
            Remove-Item -Path $BUNDLE_NAME -Recurse -Force
        }
        
        # Create bundle directory
        New-Item -ItemType Directory -Path $BUNDLE_NAME | Out-Null
        
        # Find and copy JAR file
        $jarFile = Get-ChildItem -Path "target" -Filter "*.jar" | 
                  Where-Object { $_.Name -notmatch "(sources|javadoc).jar$" } |
                  Select-Object -First 1
        
        if (-not $jarFile) {
            Write-LogError "JAR file not found in target directory"
            return $false
        }
        
        Copy-Item $jarFile.FullName -Destination "$BUNDLE_NAME\app.jar"
        
        # Copy and process run.bat template
        $templatePath = Join-Path $SCRIPT_DIR "run.bat.template"
        if (-not (Test-Path $templatePath)) {
            Write-LogError "run.bat.template not found at: $templatePath"
            return $false
        }
        
        Copy-Item $templatePath -Destination "$BUNDLE_NAME\run.bat"
        
        Write-LogInfo "Bundle structure created successfully"
        return $true
    }
    catch {
        Write-LogError "Failed to create bundle structure: $_"
        return $false
    }
}

# Function to create the final bundle archive
function New-BundleArchive {
    Write-LogInfo "Creating bundle archive..."
    
    try {
        Compress-Archive -Path $BUNDLE_NAME -DestinationPath "$BUNDLE_NAME.zip" -Force
        Write-LogInfo "Bundle archive created successfully: $BUNDLE_NAME.zip"
        return $true
    }
    catch {
        Write-LogError "Failed to create zip archive: $_"
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
    
    try {
        if (-not (Build-Application)) {
            throw "Build failed"
        }
        
        if (-not (New-BundleStructure)) {
            throw "Bundle structure creation failed"
        }
        
        if (-not (New-BundleArchive)) {
            throw "Bundle archive creation failed"
        }
        
        Write-LogInfo "Bundle creation completed successfully"
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