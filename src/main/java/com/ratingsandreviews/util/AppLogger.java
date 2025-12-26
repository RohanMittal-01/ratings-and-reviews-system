package com.ratingsandreviews.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Logger utility class using Singleton pattern.
 * Provides centralized logging functionality using SLF4J with Log4j2 implementation.
 * Thread-safe implementation using lazy initialization with ConcurrentHashMap.
 */
public final class AppLogger {

    private static final ConcurrentHashMap<String, AppLogger> LOGGER_CACHE = new ConcurrentHashMap<>();
    private final Logger logger;

    /**
     * Private constructor to prevent direct instantiation.
     * 
     * @param clazz the class for which the logger is created
     */
    private AppLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Private constructor for named loggers.
     * 
     * @param name the name of the logger
     */
    private AppLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    /**
     * Get logger instance for a specific class.
     * Uses thread-safe lazy initialization with caching.
     * 
     * @param clazz the class for which to get the logger
     * @return AppLogger instance
     */
    public static AppLogger getInstance(Class<?> clazz) {
        String className = clazz.getName();
        return LOGGER_CACHE.computeIfAbsent(className, k -> new AppLogger(clazz));
    }

    /**
     * Get logger instance with a specific name.
     * 
     * @param name the logger name
     * @return AppLogger instance
     */
    public static AppLogger getInstance(String name) {
        return LOGGER_CACHE.computeIfAbsent(name, k -> new AppLogger(name));
    }

    /**
     * Log a message at TRACE level.
     * 
     * @param message the message to log
     */
    public void trace(String message) {
        logger.trace(message);
    }

    /**
     * Log a message at TRACE level with arguments.
     * 
     * @param message the message format
     * @param args    the arguments
     */
    public void trace(String message, Object... args) {
        logger.trace(message, args);
    }

    /**
     * Log a message at DEBUG level.
     * 
     * @param message the message to log
     */
    public void debug(String message) {
        logger.debug(message);
    }

    /**
     * Log a message at DEBUG level with arguments.
     * 
     * @param message the message format
     * @param args    the arguments
     */
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * Log a message at INFO level.
     * 
     * @param message the message to log
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * Log a message at INFO level with arguments.
     * 
     * @param message the message format
     * @param args    the arguments
     */
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    /**
     * Log a message at WARN level.
     * 
     * @param message the message to log
     */
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * Log a message at WARN level with arguments.
     * 
     * @param message the message format
     * @param args    the arguments
     */
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    /**
     * Log a message at WARN level with throwable.
     * 
     * @param message the message to log
     * @param t       the throwable
     */
    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }

    /**
     * Log a message at ERROR level.
     * 
     * @param message the message to log
     */
    public void error(String message) {
        logger.error(message);
    }

    /**
     * Log a message at ERROR level with arguments.
     * 
     * @param message the message format
     * @param args    the arguments
     */
    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    /**
     * Log a message at ERROR level with throwable.
     * 
     * @param message the message to log
     * @param t       the throwable
     */
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    /**
     * Check if TRACE level is enabled.
     * 
     * @return true if TRACE is enabled
     */
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    /**
     * Check if DEBUG level is enabled.
     * 
     * @return true if DEBUG is enabled
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Check if INFO level is enabled.
     * 
     * @return true if INFO is enabled
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Check if WARN level is enabled.
     * 
     * @return true if WARN is enabled
     */
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Check if ERROR level is enabled.
     * 
     * @return true if ERROR is enabled
     */
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }
}
