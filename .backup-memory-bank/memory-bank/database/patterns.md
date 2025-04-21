# Database Patterns

## Database Architecture

### Oracle Database
1. **Container Configuration**
   - Container Name: oracledb
   - PDB: FREEPDB1
   - System Schema: Administrative access
   - HR Schema: Application data
   - Connection Pattern: `sqlplus user/pass@localhost:1521/FREEPDB1`

2. **Access Patterns**
   ```makefile
   # System Access
   @docker exec oracledb sqlplus -S system/SecurePassword@localhost:1521/FREEPDB1
   
   # HR Access
   @docker exec oracledb sqlplus -S hr/HR@localhost:1521/FREEPDB1
   ```

3. **Initialization Pattern**
   ```makefile
   # Readiness Check
   @docker logs oracledb 2>&1 | grep "DATABASE IS READY TO USE!"
   
   # PDB Check
   @docker logs oracledb 2>&1 | grep "FREEPDB1 IS READY TO USE"
   ```

### PostgreSQL Database
1. **Container Configuration**
   - Container Name: postgresdb
   - Admin User: postgres
   - HR Schema: Mirror of Oracle HR schema
   - Connection Pattern: `psql -U user`

2. **Access Patterns**
   ```makefile
   # Admin Access
   @docker exec postgresdb psql -U postgres
   
   # HR Access
   @docker exec postgresdb psql -U hr
   ```

3. **Initialization Pattern**
   ```makefile
   # Readiness Check
   @docker logs postgresdb 2>&1 | grep "database system is ready"
   ```

## Testing Patterns

### Hierarchical Testing
```makefile
test-all-db
├── test-oracle-connections-all
│   ├── test-oracle-system
│   └── test-oracle-hr
└── test-postgres-connections-all
    ├── test-postgres-admin
    └── test-postgres-hr
```

### Test Types
1. **Connection Tests**
   - Basic connectivity
   - Authentication
   - Schema access

2. **Schema Tests**
   - Table existence
   - Data access
   - Permission verification

3. **Integration Tests**
   - Cross-database operations
   - Schema synchronization
   - Data consistency

## Initialization Sequence

### Oracle Database
1. Container startup
2. Database instance initialization
3. PDB (FREEPDB1) startup
4. HR schema setup
5. Ready for connections

### PostgreSQL Database
1. Container startup
2. Database system initialization
3. User creation
4. Schema setup
5. Ready for connections

## Best Practices

1. **Connection Management**
   - Use silent mode (-S) for cleaner output
   - Handle authentication securely
   - Verify connections before operations

2. **Schema Management**
   - Keep Oracle and PostgreSQL schemas synchronized
   - Use consistent naming conventions
   - Maintain parallel permission structures

3. **Testing Strategy**
   - Test both admin and application access
   - Verify schema-level operations
   - Ensure cross-database consistency 