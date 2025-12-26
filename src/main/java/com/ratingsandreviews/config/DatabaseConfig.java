package com.ratingsandreviews.config;

import com.ratingsandreviews.util.AppLogger;
import com.ratingsandreviews.util.DBConnection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Database configuration class.
 * Configures the DataSource bean using the DBConnection singleton utility.
 * This configuration is only active when using PostgreSQL (not for tests with H2).
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "org.postgresql.Driver")
public class DatabaseConfig {

    private static final AppLogger logger = AppLogger.getInstance(DatabaseConfig.class);

    /**
     * Create and configure DataSource bean.
     * Uses the DBConnection singleton utility for connection management.
     * 
     * @return DataSource instance
     */
    @Bean
    public DataSource dataSource() {
        logger.info("Configuring DataSource using DBConnection utility");
        DBConnection dbConnection = DBConnection.getInstance();
        
        // Log connection pool status
        logger.info("Database connection pool initialized - Total connections: {}, Active: {}, Idle: {}",
                dbConnection.getTotalConnections(),
                dbConnection.getActiveConnections(),
                dbConnection.getIdleConnections());
        
        // Validate connection
        if (dbConnection.isConnectionValid()) {
            logger.info("Database connection validation successful");
        } else {
            logger.error("Database connection validation failed");
        }
        
        return dbConnection.getDataSource();
    }
}
