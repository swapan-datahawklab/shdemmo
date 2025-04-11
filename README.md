# Database Utility Tool

## Logging Configuration

### Overview
The application uses a two-tier logging system:
1. Regular application logging (`logger`)
2. Method-specific detailed logging (`methodLogger`)

### Log Levels
- **TRACE**: Detailed operation information (SQL queries, parameter values)
- **DEBUG**: Operation lifecycle events (method entry/exit, state changes)
- **INFO**: Important state changes and operation results
- **ERROR**: Failures with detailed context

### Configuration

#### Method-Specific Debug Logging
To enable detailed method-level logging, add the following to your `logback.xml` or `log4j2.xml`:

```xml
<!-- Regular application logging -->
<logger name="com.example.shelldemo" level="INFO"/>

<!-- Method-specific detailed logging -->
<logger name="com.example.shelldemo.UnifiedDatabaseRunner.methods" level="DEBUG">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="METHOD_DEBUG_FILE"/>
</logger>
```

#### Log File Configuration
```xml
<!-- Regular log file -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<!-- Method debug log file -->
<appender name="METHOD_DEBUG_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/method-debug.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/method-debug.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level - %msg%n</pattern>
    </encoder>
</appender>
```

### Available Debug Information

#### Database Operations
- Script execution progress
- SQL statement details
- Statement execution status
- Parameter values (masked for sensitive data)
- Execution times

#### Stored Procedures
- Parameter parsing details
- Execution progress
- Return values
- Error context

#### Configuration
- Database connection details
- Configuration validation
- Driver loading status

### Example Log Output

Regular application log:
```
2024-03-15 10:30:45 [main] INFO  UnifiedDatabaseRunner - Executing SQL script: /path/to/script.sql
2024-03-15 10:30:46 [main] INFO  UnifiedDatabaseRunner - Script execution completed successfully - 5 statements executed
```

Method-specific debug log:
```
2024-03-15 10:30:45 [main] DEBUG [executeScript] Starting execution of script: script.sql
2024-03-15 10:30:45 [main] DEBUG [executeScript] Parsed 5 SQL statements from file
2024-03-15 10:30:45 [main] DEBUG [executeScript] Processing statement 1/5
2024-03-15 10:30:45 [main] DEBUG [executeScript] Statement 1/5 executed successfully
```

### Enabling Debug Logging

1. **Production Environment**:
   - Set regular logging to `INFO`
   - Disable method-specific logging or set to `ERROR`

2. **Development Environment**:
   - Set regular logging to `DEBUG`
   - Set method-specific logging to `DEBUG` or `TRACE`

3. **Troubleshooting**:
   - Enable method-specific logging: `DEBUG` or `TRACE`
   - Check `logs/method-debug.log` for detailed execution flow

### Performance Considerations
- Method-specific logging at `TRACE` level may impact performance
- Use `DEBUG` level for general troubleshooting
- In production, keep method-specific logging at `ERROR` or disabled

### Log File Locations
- Regular logs: `logs/application.log`
- Method debug logs: `logs/method-debug.log`
- Rolling policy: Daily rotation with compression
- Retention: 30 days for regular logs, 7 days for debug logs

ssh -i "C:\Users\swapa\.ssh\id_rsz_tinypcwsl" swapanc@tinypcwsl -p 2222

ssh swapanc@tinypcwsl -p 2222