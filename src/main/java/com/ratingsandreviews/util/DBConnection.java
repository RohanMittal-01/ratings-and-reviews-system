package com.ratingsandreviews.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Connection utility class using Singleton pattern.
 * Provides centralized PostgreSQL database connection management using HikariCP connection pool.
 * Thread-safe implementation using Bill Pugh Singleton Design (lazy initialization with inner static class).
 */
public final class DBConnection {

    private static final AppLogger logger = AppLogger.getInstance(DBConnection.class);
    
    private final HikariDataSource dataSource;

    /**
     * Private constructor to prevent direct instantiation.
     * Initializes HikariCP connection pool with configuration.
     */
    private DBConnection() {
        logger.info("Initializing DBConnection singleton instance");
        
        HikariConfig config = new HikariConfig();
        
        // Database connection properties
        config.setJdbcUrl(getProperty("DB_URL", "jdbc:postgresql://localhost:5432/ratings_reviews"));
        config.setUsername(getProperty("DB_USERNAME", "postgres"));
        config.setPassword(getProperty("DB_PASSWORD", "postgres"));
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool configuration for high availability
        config.setMaximumPoolSize(Integer.parseInt(getProperty("DB_POOL_SIZE", "10")));
        config.setMinimumIdle(Integer.parseInt(getProperty("DB_MIN_IDLE", "5")));
        config.setConnectionTimeout(Long.parseLong(getProperty("DB_CONNECTION_TIMEOUT", "30000")));
        config.setIdleTimeout(Long.parseLong(getProperty("DB_IDLE_TIMEOUT", "600000")));
        config.setMaxLifetime(Long.parseLong(getProperty("DB_MAX_LIFETIME", "1800000")));
        
        // Performance and reliability settings
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("RatingsAndReviewsPool");
        
        // Additional PostgreSQL optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("reWriteBatchedInserts", "true");
        
        // Enable leak detection in development
        config.setLeakDetectionThreshold(Long.parseLong(getProperty("DB_LEAK_DETECTION_THRESHOLD", "60000")));
        
        this.dataSource = new HikariDataSource(config);
        logger.info("HikariCP connection pool initialized successfully");
    }

    /**
     * Helper method to get property from system properties or environment variables with default fallback.
     * 
     * @param key          the property key
     * @param defaultValue the default value if property is not found
     * @return the property value or default
     */
    private String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value != null ? value : defaultValue;
    }

    /**
     * Bill Pugh Singleton implementation using inner static class.
     * Ensures lazy initialization and thread-safety without synchronization.
     */
    private static class SingletonHelper {
        private static final DBConnection INSTANCE = new DBConnection();
    }

    /**
     * Get the singleton instance of DBConnection.
     * 
     * @return DBConnection instance
     */
    public static DBConnection getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Get a database connection from the pool.
     * 
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        logger.debug("Retrieving database connection from pool");
        return dataSource.getConnection();
    }

    /**
     * Get the underlying HikariCP DataSource.
     * 
     * @return DataSource instance
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Check if the database connection is valid.
     * 
     * @return true if connection is valid, false otherwise
     */
    public boolean isConnectionValid() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Database connection validation failed", e);
            return false;
        }
    }

    /**
     * Get current active connections count.
     * 
     * @return number of active connections
     */
    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    /**
     * Get current idle connections count.
     * 
     * @return number of idle connections
     */
    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }

    /**
     * Get total connections count.
     * 
     * @return total number of connections
     */
    public int getTotalConnections() {
        return dataSource.getHikariPoolMXBean().getTotalConnections();
    }

    /**
     * Close the connection pool and release all resources.
     * Should be called on application shutdown.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool");
            dataSource.close();
            logger.info("Database connection pool closed successfully");
        }
    }

    /**
     * Prevent cloning of singleton instance.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cannot clone singleton instance");
    }
}
