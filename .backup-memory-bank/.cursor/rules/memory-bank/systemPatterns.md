# System Patterns

## Architecture
- Command-line interface pattern using Picocli
- Database access layer for Oracle operations
- Logging system for operation tracking
- Configuration management using YAML

## Design Patterns
- Command Pattern for CLI operations
- Factory Pattern for database connections
- Builder Pattern for complex objects
- Strategy Pattern for different database operations

## Component Relationships
- CLI Layer → Command Handlers
- Command Handlers → Database Layer
- Logging across all layers
- Configuration management accessible to all components

## Code Organization
- Main application code in src/main/java
- Test code in src/test/java
- Resources and configurations in src/main/resources