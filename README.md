# Oracle Database CLI Tool

## Using Oracle with default driver loading

```bash
java -jar your-app.jar db -t oracle -H localhost -u user -p pass -d dbname script.sql
```

## Using SQL Server with a specific driver

```bash
java -jar your-app.jar db -t sqlserver -H localhost -u user -p pass -d dbname --driver-path /path/to/sqljdbc.jar script.sql
```

## Using PostgreSQL with custom port

```bash
java -jar your-app.jar db -t postgresql -H localhost -P 5433 -u user -p pass -d dbname script.sql
````

## Using MySQL with stored procedure

```bash
java -jar your-app.jar db -t mysql -H localhost -u user -p pass -d dbname procedure_name -i "param1:VARCHAR:value1"
```
