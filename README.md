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

docker run -d -p 1521:1521 -e ORACLE_PASSWORD=Tublu1224 -v oracle-volume:/opt/oracle/oradata  dock
docker commit abbcad1afa51 swapan-datahawklab/oracledb:v1
docker run -d -p 1521:1521  -e ORACLE_PASSWORD=Tublu1224 -v oracle-volume:/opt/oracle/oradata swapan-datahawklab/oracledb:v1

java -jar target/shdemmo-1.0-SNAPSHOT.jar -t oracle -H localhost -P 1521 -u hr -p hr -d freepdb1 --print-statements true test.sql

java -jar target/shdemmo-1.0-SNAPSHOT.jar -t oracle -H 172.17.0.2 -P 1521 -u hr -p hr -d freepdb1 --print-statements test1.sql
jdbc:oracle:thin:@//172.17.0.2:1521/freepdb1?SERVICE_NAME=freepdb1


/home/swapanc/shdemmo-1/shdemmo-bundle-linux/runtime/bin/java \
-jar /home/swapanc/shdemmo-1/shdemmo-bundle-linux/app/shdemmo-1.0-SNAPSHOT.jar \
-t oracle \
-H 172.17.0.2 \
-P 1521 \
-u hr \
-p hr \
-d freepdb1 \
--print-statements \
/home/swapanc/shdemmo-1/src/main/resources/oracle_init_scripts/test.sql

