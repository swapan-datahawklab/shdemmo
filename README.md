# Oracle Database CLI Tool

A command-line tool for executing Oracle SQL scripts and stored procedures.

## Prerequisites

- Java 21 or later
- Oracle JDBC driver
- Oracle database access

## Building

```bash
mvn clean package
```

## Usage Examples

### Running SQL Scripts

```bash
# Basic script execution
java -jar target/shdemmo-1.jar script -H localhost:1521/ORCL -u username -p password script.sql

# With additional options
java -jar target/shdemmo-1.jar script -H localhost:1521/ORCL -u username -p password script.sql \
  --stop-on-error=true \
  --auto-commit=false \
  --print-statements=true
```

### Running Stored Procedures

```bash
# Basic procedure call
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password UPDATE_EMPLOYEE_SALARY \
  -i "p_emp_id:NUMERIC:101,p_percentage:NUMERIC:10.5" \
  -o "p_new_salary:NUMERIC"

# Function call
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password GET_DEPARTMENT_BUDGET \
  --function \
  --return-type NUMERIC \
  -i "p_dept_id:NUMERIC:20"

# Procedure with INOUT parameter
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password CALCULATE_BONUS \
  -i "p_salary:NUMERIC:50000.00" \
  --io "p_bonus:NUMERIC:0.00"
```

## Parameter Types

Supported SQL types for parameters:
- NUMERIC
- VARCHAR
- DATE
- TIMESTAMP
- CLOB
- BLOB

## Parameter Format

Parameters are specified in the format: `name:type:value`

Examples:
- Input parameter: `p_emp_id:NUMERIC:101`
- Output parameter: `p_result:NUMERIC`
- INOUT parameter: `p_value:NUMERIC:100.00`

## Options

### Script Execution Options
- `--stop-on-error`: Stop execution on first error (default: true)
- `--auto-commit`: Enable auto-commit mode (default: false)
- `--print-statements`: Print SQL statements as they execute (default: false)

### Procedure Execution Options
- `--function`: Execute as a function instead of a procedure
- `--return-type`: Specify return type for functions
- `--print-output`: Print output parameters (default: true)

## Usage Analysis
To run dependency analysis:
```bash
./dependency-check.sh
```

## Manual Verification Commands
```bash
# Direct usage analysis
find src/ -type f -name "*.java" | xargs grep -l "oracle.jdbc"
find src/ -type f -name "*.java" | xargs grep -l "org.slf4j"
find src/ -type f -name "*.java" | xargs grep -l "org.junit"
find src/ -type f -name "*.java" | xargs grep -l "org.mockito"
find src/ -type f -name "*.java" | xargs grep -l "com.zaxxer.hikari"
find src/ -type f -name "*.java" | xargs grep -l "picocli"

# Maven dependency analysis
mvn dependency:analyze
mvn dependency:tree
mvn dependency:analyze-duplicate
```

## Current Dependencies
- ojdbc11: Oracle database connectivity
- logback-classic: SLF4J logging implementation
- junit-jupiter: Unit testing (test scope)
- mockito-junit-jupiter: Mocking in tests (test scope)
- HikariCP: Connection pooling
- picocli: Command-line interface
EOF

## Dependencies and Database Connectivity

### Core Dependencies
```xml
<dependencies>
    <!-- Oracle JDBC Driver -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc11</artifactId>
        <version>${oracle.version}</version>
    </dependency>

    <!-- Connection Pooling -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>${hikaricp.version}</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
    </dependency>

    <!-- Command Line Interface -->
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli</artifactId>
        <version>${picocli.version}</version>
    </dependency>
</dependencies>
```

### Dependency Analysis

#### Available Commands
1. Basic dependency analysis:
```bash
mvn dependency:analyze
```
Shows:
- Used and declared dependencies
- Used but undeclared dependencies
- Unused but declared dependencies

2. View dependency tree:
```bash
mvn dependency:tree
```

3. Runtime dependency verification:
```bash
mvn dependency:analyze-only -DoutputXML=true -DignoreNonCompile=false
```

#### Dependency Usage Verification
To verify specific dependency usage:
```bash
# Oracle JDBC usage
find src/ -type f -name "*.java" | xargs grep -l "oracle.jdbc"

# Logging usage (SLF4J/Logback)
find src/ -type f -name "*.java" | xargs grep -l "org.slf4j"

# Test framework usage
find src/ -type f -name "*.java" | xargs grep -l "org.junit"
find src/ -type f -name "*.java" | xargs grep -l "org.mockito"
```

### Understanding Dependency Analysis Results

#### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)

```java
// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
# Oracle Database CLI Tool

A command-line tool for executing Oracle SQL scripts and stored procedures.

## Prerequisites

- Java 21 or later
- Oracle JDBC driver
- Oracle database access

## Building

```bash
mvn clean package
```

## Usage Examples

### Running SQL Scripts

```bash
# Basic script execution
java -jar target/shdemmo-1.jar script -H localhost:1521/ORCL -u username -p password script.sql

# With additional options
java -jar target/shdemmo-1.jar script -H localhost:1521/ORCL -u username -p password script.sql \
  --stop-on-error=true \
  --auto-commit=false \
  --print-statements=true
```

### Running Stored Procedures

```bash
# Basic procedure call
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password UPDATE_EMPLOYEE_SALARY \
  -i "p_emp_id:NUMERIC:101,p_percentage:NUMERIC:10.5" \
  -o "p_new_salary:NUMERIC"

# Function call
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password GET_DEPARTMENT_BUDGET \
  --function \
  --return-type NUMERIC \
  -i "p_dept_id:NUMERIC:20"

# Procedure with INOUT parameter
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password CALCULATE_BONUS \
  -i "p_salary:NUMERIC:50000.00" \
  --io "p_bonus:NUMERIC:0.00"
```

## Parameter Types

Supported SQL types for parameters:
- NUMERIC
- VARCHAR
- DATE
- TIMESTAMP
- CLOB
- BLOB

## Parameter Format

Parameters are specified in the format: `name:type:value`

Examples:
- Input parameter: `p_emp_id:NUMERIC:101`
- Output parameter: `p_result:NUMERIC`
- INOUT parameter: `p_value:NUMERIC:100.00`

## Options

### Script Execution Options
- `--stop-on-error`: Stop execution on first error (default: true)
- `--auto-commit`: Enable auto-commit mode (default: false)
- `--print-statements`: Print SQL statements as they execute (default: false)

### Procedure Execution Options
- `--function`: Execute as a function instead of a procedure
- `--return-type`: Specify return type for functions
- `--print-output`: Print output parameters (default: true)

## Usage Analysis
To run dependency analysis:
```bash
./dependency-check.sh
```

## Manual Verification Commands
```bash
# Direct usage analysis
find src/ -type f -name "*.java" | xargs grep -l "oracle.jdbc"
find src/ -type f -name "*.java" | xargs grep -l "org.slf4j"
find src/ -type f -name "*.java" | xargs grep -l "org.junit"
find src/ -type f -name "*.java" | xargs grep -l "org.mockito"
find src/ -type f -name "*.java" | xargs grep -l "com.zaxxer.hikari"
find src/ -type f -name "*.java" | xargs grep -l "picocli"

# Maven dependency analysis
mvn dependency:analyze
mvn dependency:tree
mvn dependency:analyze-duplicate
```

## Current Dependencies
- ojdbc11: Oracle database connectivity
- logback-classic: SLF4J logging implementation
- junit-jupiter: Unit testing (test scope)
- mockito-junit-jupiter: Mocking in tests (test scope)
- HikariCP: Connection pooling
- picocli: Command-line interface
EOF

## Dependencies and Database Connectivity

### Core Dependencies
```xml
<dependencies>
    <!-- Oracle JDBC Driver -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc11</artifactId>
        <version>${oracle.version}</version>
    </dependency>

    <!-- Connection Pooling -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>${hikaricp.version}</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
    </dependency>

    <!-- Command Line Interface -->
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli</artifactId>
        <version>${picocli.version}</version>
    </dependency>
</dependencies>
```

### Dependency Analysis

#### Available Commands
1. Basic dependency analysis:
```bash
mvn dependency:analyze
```
Shows:
- Used and declared dependencies
- Used but undeclared dependencies
- Unused but declared dependencies

2. View dependency tree:
```bash
mvn dependency:tree
```

3. Runtime dependency verification:
```bash
mvn dependency:analyze-only -DoutputXML=true -DignoreNonCompile=false
```

#### Dependency Usage Verification
To verify specific dependency usage:
```bash
# Oracle JDBC usage
find src/ -type f -name "*.java" | xargs grep -l "oracle.jdbc"

# Logging usage (SLF4J/Logback)
find src/ -type f -name "*.java" | xargs grep -l "org.slf4j"

# Test framework usage
find src/ -type f -name "*.java" | xargs grep -l "org.junit"
find src/ -type f -name "*.java" | xargs grep -l "org.mockito"
```

### Understanding Dependency Analysis Results

#### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties
```

Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)


// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}

```

// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```

## Public API Usage

Instead of using reflection for field access, use proper encapsulation with getters/setters:

```java
class MyClass {
    // Use proper encapsulation with getters/setters
    private String someField;
    
    public String getSomeField() {
        return someField;
    }
    
    public void setSomeField(String value) {
        this.someField = value;
    }
}
```

## Dependency Verification Tests

Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

## Dependency Analysis Results

# Oracle Database CLI Tool

A command-line tool for executing Oracle SQL scripts and stored procedures.

## Prerequisites

- Java 21 or later
- Oracle JDBC driver
- Oracle database access

## Building

```bash
mvn clean package
```

## Usage Examples

### Running SQL Scripts

```bash
# Basic script execution
java -jar target/shdemmo-1.jar script -H localhost:1521/ORCL -u username -p password script.sql

# With additional options
java -jar target/shdemmo-1.jar script -H localhost:1521/ORCL -u username -p password script.sql \
  --stop-on-error=true \
  --auto-commit=false \
  --print-statements=true
```

### Running Stored Procedures

```bash
# Basic procedure call
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password UPDATE_EMPLOYEE_SALARY \
  -i "p_emp_id:NUMERIC:101,p_percentage:NUMERIC:10.5" \
  -o "p_new_salary:NUMERIC"

# Function call
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password GET_DEPARTMENT_BUDGET \
  --function \
  --return-type NUMERIC \
  -i "p_dept_id:NUMERIC:20"

# Procedure with INOUT parameter
java -jar target/shdemmo-1.jar proc -H localhost:1521/ORCL -u username -p password CALCULATE_BONUS \
  -i "p_salary:NUMERIC:50000.00" \
  --io "p_bonus:NUMERIC:0.00"
```

## Parameter Types

Supported SQL types for parameters:
- NUMERIC
- VARCHAR
- DATE
- TIMESTAMP
- CLOB
- BLOB

## Parameter Format

Parameters are specified in the format: `name:type:value`

Examples:
- Input parameter: `p_emp_id:NUMERIC:101`
- Output parameter: `p_result:NUMERIC`
- INOUT parameter: `p_value:NUMERIC:100.00`

## Options

### Script Execution Options
- `--stop-on-error`: Stop execution on first error (default: true)
- `--auto-commit`: Enable auto-commit mode (default: false)
- `--print-statements`: Print SQL statements as they execute (default: false)

### Procedure Execution Options
- `--function`: Execute as a function instead of a procedure
- `--return-type`: Specify return type for functions
- `--print-output`: Print output parameters (default: true)

## Usage Analysis
To run dependency analysis:
```bash
./dependency-check.sh
```

## Manual Verification Commands
```bash
# Direct usage analysis
find src/ -type f -name "*.java" | xargs grep -l "oracle.jdbc"
find src/ -type f -name "*.java" | xargs grep -l "org.slf4j"
find src/ -type f -name "*.java" | xargs grep -l "org.junit"
find src/ -type f -name "*.java" | xargs grep -l "org.mockito"
find src/ -type f -name "*.java" | xargs grep -l "com.zaxxer.hikari"
find src/ -type f -name "*.java" | xargs grep -l "picocli"

# Maven dependency analysis
mvn dependency:analyze
mvn dependency:tree
mvn dependency:analyze-duplicate
```

## Current Dependencies
- ojdbc11: Oracle database connectivity
- logback-classic: SLF4J logging implementation
- junit-jupiter: Unit testing (test scope)
- mockito-junit-jupiter: Mocking in tests (test scope)
- HikariCP: Connection pooling
- picocli: Command-line interface
EOF

## Dependencies and Database Connectivity

### Core Dependencies
```xml
<dependencies>
    <!-- Oracle JDBC Driver -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc11</artifactId>
        <version>${oracle.version}</version>
    </dependency>

    <!-- Connection Pooling -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>${hikaricp.version}</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>${logback.version}</version>
    </dependency>

    <!-- Command Line Interface -->
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli</artifactId>
        <version>${picocli.version}</version>
    </dependency>
</dependencies>
```

### Dependency Analysis

#### Available Commands
1. Basic dependency analysis:
```bash
mvn dependency:analyze
```
Shows:
- Used and declared dependencies
- Used but undeclared dependencies
- Unused but declared dependencies

2. View dependency tree:
```bash
mvn dependency:tree
```

3. Runtime dependency verification:
```bash
mvn dependency:analyze-only -DoutputXML=true -DignoreNonCompile=false
```

#### Dependency Usage Verification
To verify specific dependency usage:
```bash
# Oracle JDBC usage
find src/ -type f -name "*.java" | xargs grep -l "oracle.jdbc"

# Logging usage (SLF4J/Logback)
find src/ -type f -name "*.java" | xargs grep -l "org.slf4j"

# Test framework usage
find src/ -type f -name "*.java" | xargs grep -l "org.junit"
find src/ -type f -name "*.java" | xargs grep -l "org.mockito"
```

### Understanding Dependency Analysis Results

#### Runtime vs Compile-Time Dependencies
Some dependencies may show as "unused" in Maven's analysis despite being crucial:

1. **JDBC Drivers (ojdbc11)**
   - Loaded dynamically at runtime
   - Uses `java.sql.*` interfaces
   - Required even if not directly referenced

2. **Logging Implementation (logback-classic)**
   - Runtime implementation of SLF4J
   - Code references only SLF4J interfaces
   - Binding happens through ServiceLoader

3. **Test Dependencies**
   - JUnit and Mockito components loaded via test runtime
   - Annotations processed during test execution
   - Integration happens through reflection

### Database Connectivity Best Practices

#### DataSource Configuration
We use HikariCP for connection pooling:

```java
public class DataSourceFactory {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
```

#### Resource Management
- Use try-with-resources for automatic resource closure
- Employ prepared statements for SQL injection prevention
- Centralize configuration in factory classes
- Separate concerns between data access and business logic

### Dependency Verification Tests
Add these tests to verify runtime dependencies:

```java
@Test
public void verifyJdbcDriver() {
    assertDoesNotThrow(() -> Class.forName("oracle.jdbc.OracleDriver"));
}

@Test
public void verifyLoggingImplementation() {
    Logger logger = LoggerFactory.getLogger(getClass());
    assertThat(logger.getClass().getName()).contains("ch.qos.logback");
}
```

### Troubleshooting Dependencies
If Maven reports false positives for unused dependencies, you can configure the analysis:

```xml
<configuration>
    <ignoredUnusedDeclaredDependencies>
        <ignoredUnusedDeclaredDependency>com.oracle.database.jdbc:ojdbc11</ignoredUnusedDeclaredDependency>
        <ignoredUnusedDeclaredDependency>ch.qos.logback:logback-classic</ignoredUnusedDeclaredDependency>
    </ignoredUnusedDeclaredDependencies>
</configuration>
```

### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
<argLine>
    <!-- Mockito needs access to create proxy classes -->
    --add-opens java.base/java.lang=ALL-UNNAMED
    
    <!-- HikariCP needs access to optimize collections -->
    --add-opens java.base/java.util=ALL-UNNAMED
    
    <!-- Testing date/time operations -->
    --add-opens java.base/java.time=ALL-UNNAMED
</argLine>
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties

Yes, I'll help add explanatory comments for the JVM `--add-opens` arguments in the `pom.xml`:

```xml
<configuration>
    <argLine>
        <!-- Required for Mockito to access private fields and methods during testing -->
        --add-opens java.base/java.lang=ALL-UNNAMED
        <!-- Required for HikariCP to access collection internals and for testing collections -->
        --add-opens java.base/java.util=ALL-UNNAMED
        <!-- Required for testing temporal classes and date/time operations -->
        --add-opens java.base/java.time=ALL-UNNAMED
        <!-- Enable dynamic agent loading for Mockito and ByteBuddy -->
        -XX:+EnableDynamicAgentLoading
    </argLine>
</configuration>
```

These arguments are needed because:
1. Java modules (introduced in Java 9+) restrict reflective access by default
2. Libraries like Mockito and HikariCP need deep reflection access to core Java classes
3. Without these flags, you might see `InaccessibleObjectException` or similar reflection-related errors during testing

You might want to add this explanation to your README as well under a "Java Module System Configuration" section:

```markdown
### Java Module System Configuration

The project requires specific JVM arguments to work with Java's module system:

```xml
--add-opens java.base/java.lang=ALL-UNNAMED   # For Mockito reflection access
--add-opens java.base/java.util=ALL-UNNAMED   # For HikariCP and collection testing
--add-opens java.base/java.time=ALL-UNNAMED   # For temporal class testing
```

These are configured in the `maven-surefire-plugin` and are required because:
- Modern Java's module system restricts reflective access by default
- Testing frameworks like Mockito need to access private members
- Connection pooling (HikariCP) requires access to internal Java classes
- Unit tests may need to manipulate normally inaccessible properties


Java modules (introduced in Java 9+) restrict access between modules by default
Libraries like Mockito and HikariCP need deep reflection access to core Java classes
Without these flags, you might see InaccessibleObjectException or similar reflection-related errors during testing

'll explain each aspect in detail:
Java Module System (Project Jigsaw)

```java
// module-info.java
module my.application {
    requires java.base;         // Core Java classes
    requires java.sql;          // JDBC API
    requires com.zaxxer.hikari; // HikariCP
    
    exports com.example.api;    // Our public API
}
```

```java
// The module system helps prevent unauthorized access
SecurityManager sm = System.getSecurityManager();
if (sm != null) {
    // Module system works with SecurityManager to enforce access
    sm.checkPermission(new ReflectPermission("suppressAccessChecks"));
}
```