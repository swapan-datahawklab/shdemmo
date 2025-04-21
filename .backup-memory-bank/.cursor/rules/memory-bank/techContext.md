# Technical Context

## Core Technologies
- Java 21
- Maven for build management
- Picocli (v4.7.5) for CLI
- Oracle JDBC Driver (v23.7.0.25.01)
- SLF4J (v2.0.17) and Logback (v1.5.18) for logging
- JUnit 5 (v5.10.2) and Mockito (v5.11.0) for testing
- Testcontainers for integration testing
- PlantUML for documentation
- YAML support for configuration

## Development Setup
- Java 21 JDK required
- Maven for dependency management and building
- IDE with Java support recommended

## Technical Constraints
- Cross-platform compatibility required (Windows/Unix)
- Oracle Database compatibility
- Memory and performance considerations for database operations

## Dependencies
All major dependencies are managed through Maven:
- Oracle JDBC for database connectivity
- Logging framework (SLF4J, Logback, Log4j2)
- Testing frameworks (JUnit, Mockito, Testcontainers)
- CLI framework (Picocli)
- Configuration (Jackson YAML)