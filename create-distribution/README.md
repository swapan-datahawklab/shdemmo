# Create Distribution

This directory contains scripts and templates for creating and testing the application distribution bundle.

## Files

- `create-bundle.sh` - Creates the application distribution bundle
- `test-logging.sh` - Tests the logging functionality of the bundled application
- `logback.xml.template` - Template for the logging configuration
- `run.sh.template` - Template for the application runner script
- `README.md.template` - Template for the bundle's README file

## Usage

1. Create the distribution bundle:

   ```bash
   ./create-bundle.sh
   ```

2. Test the bundle:

   ```bash
   ./test-logging.sh
   ```

The scripts will create a `shdemmo-bundle` directory and a `shdemmo-bundle.tar.gz` file in the project root.
