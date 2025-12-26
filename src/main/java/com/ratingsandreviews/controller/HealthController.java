package com.ratingsandreviews.controller;

import com.ratingsandreviews.util.AppLogger;
import com.ratingsandreviews.util.DBConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller.
 * Provides endpoints to check application and database health status.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    private static final AppLogger logger = AppLogger.getInstance(HealthController.class);

    /**
     * Basic health check endpoint.
     * 
     * @return health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        logger.info("Health check endpoint called");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "Ratings and Reviews System");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Database health check endpoint.
     * 
     * @return database health status
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        logger.info("Database health check endpoint called");
        
        Map<String, Object> dbHealth = new HashMap<>();
        DBConnection dbConnection = DBConnection.getInstance();
        
        try {
            boolean isValid = dbConnection.isConnectionValid();
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("database", "PostgreSQL");
            dbHealth.put("totalConnections", dbConnection.getTotalConnections());
            dbHealth.put("activeConnections", dbConnection.getActiveConnections());
            dbHealth.put("idleConnections", dbConnection.getIdleConnections());
            
            logger.info("Database health check completed - Status: {}", isValid ? "UP" : "DOWN");
            
            return ResponseEntity.ok(dbHealth);
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            return ResponseEntity.status(503).body(dbHealth);
        }
    }
}
