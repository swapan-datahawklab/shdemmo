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
