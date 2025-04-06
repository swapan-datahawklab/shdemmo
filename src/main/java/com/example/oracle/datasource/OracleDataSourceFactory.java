package com.example.oracle.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Properties;

public class OracleDataSourceFactory {
    private static final String DEFAULT_POOL_SIZE = "10";
    private static final String DEFAULT_MIN_IDLE = "2";
    private static final String DEFAULT_MAX_LIFETIME = "1800000"; // 30 minutes

    public static DataSource createDataSource(String host, String username, String password) {
        HikariConfig config = new HikariConfig();
        
        // Configure basic connection properties
        config.setJdbcUrl("jdbc:oracle:thin:@" + host);
        config.setUsername(username);
        config.setPassword(password);
        
        // Configure connection pool settings
        config.setMaximumPoolSize(Integer.parseInt(System.getProperty("oracle.pool.size", DEFAULT_POOL_SIZE)));
        config.setMinimumIdle(Integer.parseInt(System.getProperty("oracle.pool.min.idle", DEFAULT_MIN_IDLE)));
        config.setMaxLifetime(Long.parseLong(System.getProperty("oracle.pool.max.lifetime", DEFAULT_MAX_LIFETIME)));
        
        // Configure Oracle-specific properties
        Properties oracleProps = new Properties();
        oracleProps.setProperty("oracle.jdbc.timezoneAsRegion", "false");
        config.setDataSourceProperties(oracleProps);
        
        return new HikariDataSource(config);
    }
} 