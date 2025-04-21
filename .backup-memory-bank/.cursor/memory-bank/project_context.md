# Project Context

## Overview
Development environment with dual database support (Oracle and PostgreSQL) for HR schema management.

## Core Components

### Database Layer
1. Oracle Database
   - Container: oracledb
   - System Schema: Administrative access
   - HR Schema: Application data
   - PDB: FREEPDB1
   - Key Tables: employees, departments, jobs
   - Connection Pattern: sqlplus user/pass@localhost:1521/FREEPDB1

2. PostgreSQL Database
   - Container: postgresdb
   - Admin User: postgres
   - HR Schema: Mirror of Oracle HR schema
   - Connection Pattern: psql -U user

### Build System
1. Make-based orchestration
   - Clean environment management
   - Container lifecycle control
   - Database initialization checks
   - Test automation

2. Docker Integration
   - Multi-container setup
   - Volume management
   - Network configuration
   - Init script processing

### Testing Framework
1. Connection Testing
   - Oracle system/HR access
   - PostgreSQL admin/HR access
   - Schema verification
   - Table access checks

2. Initialization Checks
   - Database readiness verification
   - Startup pattern analysis
   - Log monitoring
   - State documentation

## Current Implementation Patterns

### Database Access
```makefile
# Oracle Pattern
@docker exec oracledb sqlplus -S user/pass@localhost:1521/FREEPDB1

# PostgreSQL Pattern
@docker exec postgresdb psql -U user
```

### Initialization Checks
```makefile
# Oracle Ready Check
@docker logs oracledb 2>&1 | grep "DATABASE IS READY TO USE!"

# PostgreSQL Ready Check
@docker logs postgresdb 2>&1 | grep "database system is ready"
```

### Test Organization
```makefile
test-all-db
├── test-oracle-connections-all
│   ├── test-oracle-system
│   └── test-oracle-hr
└── test-postgres-connections-all
    ├── test-postgres-admin
    └── test-postgres-hr
```

## Integration Points
1. Database Containers
   - Shared network
   - Volume mounts
   - Init scripts

2. Make Tasks
   - Build dependencies
   - Test hierarchy
   - Status checks

## Configuration Requirements
1. Oracle
   - FREEPDB1 PDB
   - HR schema setup
   - System access

2. PostgreSQL
   - HR schema
   - User permissions
   - Init scripts 