# Technical Context

## Technology Stack
- Java 21
- Maven 3.x
- Oracle JDBC (version 23.7.0.25.01)
- PicoCLI (version 4.7.5)
- SLF4J (version 2.0.17) & Logback (version 1.5.18)
- Log4j2 (version 2.22.1)
- Jackson YAML (version 2.16.1)
- PlantUML (version 1.2024.3)

## Development Environment
- OS: Cross-platform (Windows and Unix support)
- IDE: VSCode with appropriate Java extensions
- Required Tools:
  * JDK 21
  * Maven
  * Git
  * Docker (for Testcontainers)

## Dependencies
- Oracle Database JDBC Driver (ojdbc11)
- Logging Framework Stack:
  * SLF4J API
  * Logback Classic
  * Log4j2 Core & API
- Testing Stack:
  * JUnit Jupiter (version 5.10.2)
  * Mockito (version 5.11.0)
  * Testcontainers (version 1.19.3)
- CLI Framework:
  * PicoCLI
- Documentation:
  * PlantUML
- Utilities:
  * Jackson YAML Parser
  * LMAX Disruptor (version 3.4.4)

## Build Process
1. Maven clean install
2. Annotation processing for PicoCLI
3. Unit tests execution (currently skipped in configuration)
4. Packaging with Maven Shade Plugin
5. Distribution bundle creation

## Deployment
- Creates executable uber-jar with all dependencies
- Generates platform-specific distribution bundles
- Supports both Windows and Unix environments

## Technical Constraints
- Requires Java 21 runtime
- Oracle Database compatibility requirements
- Cross-platform compatibility considerations
- Memory and performance requirements for database operations

## Testing Strategy
- Unit Testing with JUnit Jupiter
- Mock testing with Mockito
- Integration testing with Testcontainers
- Oracle XE container for database testing
- Continuous Integration testing

## Monitoring & Logging
- Comprehensive logging with SLF4J, Logback, and Log4j2
- Asynchronous logging with LMAX Disruptor
- Configurable log levels and outputs
- Performance monitoring capabilities

Note: Keep this document updated with technical changes. 