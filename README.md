# Shell Demo Application
A command-line application built with Java and Picocli that demonstrates modern CLI development practices.

.\create-distribution\create-bundle-windows.ps1
.\shdemmo-bundle-windows\run.bat

## Features

- Command-line interface with built-in help and version information
- Configurable greeting message
- Verbose output mode
- Structured logging using SLF4J and Logback
- Built as a self-contained executable JAR

## Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

## Building the Application

To build the application, run:

```bash
mvn clean package
```

This will create an executable JAR file in the `target` directory.

## Running the Application

After building, you can run the application using:

```bash
java -jar target/shdemmo-1.0-SNAPSHOT.jar
```

### Command-line Options

- `-n, --name <name>`: Specify the name to greet (default: "World")
- `-v, --verbose`: Enable verbose output
- `-h, --help`: Show help message and exit
- `-V, --version`: Display version information

### Examples

```bash
# Basic greeting
java -jar target/shdemmo-1.0-SNAPSHOT.jar
# Output: Hello, World!

# Custom greeting
java -jar target/shdemmo-1.0-SNAPSHOT.jar --name Alice
# Output: Hello, Alice!

# Verbose mode
java -jar target/shdemmo-1.0-SNAPSHOT.jar -v
```

## Running the Bundled Application

After creating the bundle using `create-bundle.sh`, you can run the application using the provided `run.sh` script in the bundle directory:

```bash
# Basic usage (default logging mode)
./run.sh

# Run with debug logging
./run.sh --log-mode debug
# or
./run.sh -l debug

# Run with trace logging (most verbose)
./run.sh --log-mode trace

# Run with minimal logging
./run.sh --log-mode quiet

# Pass additional Java arguments after --
./run.sh -- --name "Test User"

# Combine logging mode with Java arguments
./run.sh --log-mode debug -- --name "Test User"

# Show help message
./run.sh --help
```

The `run.sh` script supports different logging modes:
- `default`: Normal application logging (INFO level)
- `debug`: Enable debug logging (DEBUG level)
- `trace`: Enable trace logging with logback debug output (TRACE level)
- `quiet`: Minimal logging (WARN level)

All logs are written to the `logs` directory within the bundle.

## Project Structure

```bash
src/
├── main/
│   ├── java/
│   │   └── com/example/shelldemo/
│   │       └── App.java
│   └── resources/
│       └── logback.xml
```

## Technical Details

### Maven Shade Plugin Configuration

The application uses the Maven Shade Plugin to create a self-contained executable JAR. Here's a detailed explanation of the transformers used in the build process:

#### 1. ManifestResourceTransformer

```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
    <mainClass>com.example.shelldemo.App</mainClass>
</transformer>
```

- Sets the main class in the JAR's manifest file
- Makes the JAR executable using `java -jar`
- Merges manifest entries from dependencies

#### 2. ServicesResourceTransformer

```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
```

- Merges Service Provider Interface (SPI) configurations
- Combines service provider files from META-INF/services/
- Essential for Java's ServiceLoader mechanism
- Ensures proper functionality of logging implementations

#### 3. ApacheLicenseResourceTransformer

```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.ApacheLicenseResourceTransformer"/>
```

- Merges LICENSE files from Apache-licensed dependencies
- Maintains legal compliance
- Preserves license information in the final JAR

#### 4. ApacheNoticeResourceTransformer

```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformer">
    <addHeader>false</addHeader>
</transformer>
```

- Merges NOTICE files from Apache-licensed dependencies
- Preserves attribution notices and acknowledgments
- Maintains compliance with Apache License requirements

### Dependencies

- **Picocli**: Command-line interface parsing
- **SLF4J**: Logging facade
- **Logback**: Logging implementation
- **JUnit Jupiter**: Testing framework
- **Mockito**: Mocking framework for tests

## Development

### Logging

The application uses SLF4J with Logback for logging. The logging configuration can be found in `src/main/resources/logback.xml`.

### Testing

Run the tests using:

```bash
mvn test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
