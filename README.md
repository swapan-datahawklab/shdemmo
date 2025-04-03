# Shell Demo Application

A command-line application built with Java and Picocli that demonstrates modern CLI development practices.

## Features

- Command-line interface with built-in help and version information
- Configurable greeting message
- Verbose output mode
- Structured logging using SLF4J and Logback
- Custom runtime image creation using jlink
- Platform-specific distribution bundles

## Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

## Building the Application

You have two build options depending on your needs:

### Option 1: Basic Build (JAR only)

```bash
mvn clean package
```

This will:

- Clean the project
- Compile the code
- Run tests
- Create an executable JAR file in the `target` directory

### Option 2: Complete Distribution Build

```bash
mvn clean verify
```

This will:

- Do everything that `package` does
- Create a custom runtime image using jlink
- Generate platform-specific distribution bundles
- Create distribution archives (tar.gz/zip)

## Running the Application

### Running from JAR (Development)

After building with `mvn package`, you can run the application using:

```bash
java -jar target/shdemmo-1.0-SNAPSHOT.jar
```

### Running from Distribution Bundle

After building with `mvn verify`, extract the appropriate bundle for your platform:

**Linux/macOS:**

```bash
tar xzf shdemmo-bundle-linux.tar.gz
cd shdemmo-bundle-linux
./run.sh
```

**Windows:**

```powershell
# Extract the ZIP file and navigate to the bundle directory
cd shdemmo-bundle-windows
.\run.bat
```

### Command-line Options

- `-n, --name <n>`: Specify the name to greet (default: "World")
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

## Logging Configuration

The application uses SLF4J with Logback for logging. Logs are written to the `logs` directory by default.

### Log Directory Configuration

You can customize the log directory location by setting the `app.log.dir` system property:

```bash
java -Dapp.log.dir=/path/to/logs -jar target/shdemmo-1.0-SNAPSHOT.jar
```

### Log Levels

- Default log level is INFO
- Can be overridden using the `root.level` system property:

```bash
java -Droot.level=DEBUG -jar target/shdemmo-1.0-SNAPSHOT.jar
```

## Project Structure

```bash
src/
├── main/
│   ├── java/
│   │   └── com/example/shelldemo/
│   │       └── App.java
│   └── resources/
│       └── logback.xml
├── create-distribution/
│   ├── create-bundle.sh         # Unix bundle creation script
│   ├── create-bundle-windows.ps1 # Windows bundle creation script
│   ├── logback.xml.template     # Logging configuration template
│   ├── run.sh.template         # Unix launcher template
│   └── README.md.template      # Bundle README template
```

## Technical Details

### Distribution Bundle Creation

The project uses Maven profiles and the `maven-jlink-plugin` to create custom runtime images and distribution bundles. The process includes:

1. Creating a minimal JRE using jlink
2. Packaging application JAR and dependencies
3. Generating platform-specific launch scripts
4. Creating distribution archives

### Dependencies

- **Picocli**: Command-line interface parsing
- **SLF4J**: Logging facade
- **Logback**: Logging implementation
- **JUnit Jupiter**: Testing framework
- **Mockito**: Mocking framework for tests

## Development

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
