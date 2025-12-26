package com.ratingsandreviews.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AppLogger utility.
 */
class AppLoggerTest {

    @Test
    void testGetInstanceReturnsNonNull() {
        AppLogger logger = AppLogger.getInstance(AppLoggerTest.class);
        assertNotNull(logger, "Logger instance should not be null");
    }

    @Test
    void testGetInstanceReturnsSameInstance() {
        AppLogger logger1 = AppLogger.getInstance(AppLoggerTest.class);
        AppLogger logger2 = AppLogger.getInstance(AppLoggerTest.class);
        assertSame(logger1, logger2, "Should return the same singleton instance");
    }

    @Test
    void testGetInstanceByName() {
        AppLogger logger = AppLogger.getInstance("TestLogger");
        assertNotNull(logger, "Logger instance by name should not be null");
    }

    @Test
    void testLoggerMethods() {
        AppLogger logger = AppLogger.getInstance(AppLoggerTest.class);
        
        // These should not throw exceptions
        assertDoesNotThrow(() -> logger.info("Test info message"));
        assertDoesNotThrow(() -> logger.debug("Test debug message"));
        assertDoesNotThrow(() -> logger.warn("Test warn message"));
        assertDoesNotThrow(() -> logger.error("Test error message"));
        assertDoesNotThrow(() -> logger.trace("Test trace message"));
    }

    @Test
    void testLoggerLevelChecks() {
        AppLogger logger = AppLogger.getInstance(AppLoggerTest.class);
        
        // Verify level check methods work
        assertNotNull(logger.isInfoEnabled());
        assertNotNull(logger.isDebugEnabled());
        assertNotNull(logger.isWarnEnabled());
        assertNotNull(logger.isErrorEnabled());
        assertNotNull(logger.isTraceEnabled());
    }
}
