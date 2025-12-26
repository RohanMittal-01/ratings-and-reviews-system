# Architecture and Design Patterns

## Overview

This document describes the architecture, design patterns, and technical decisions for the Ratings and Reviews System.

## Architecture

### Layered Architecture

The application follows a standard layered architecture pattern:

```
Presentation Layer (Controller)
    ↓
Business Logic Layer (Service)
    ↓
Data Access Layer (Repository)
    ↓
Database Layer (PostgreSQL Cluster)
```

### Package Structure

```
com.ratingsandreviews/
├── config/              # Spring configuration classes
├── controller/          # REST API controllers (Presentation Layer)
├── service/            # Business logic services (Service Layer)
├── repository/         # Data access repositories (Data Access Layer)
├── model/              # Domain models and entities
├── util/               # Utility classes (Logger, DBConnection)
└── RatingsAndReviewsApplication.java
```

## Design Patterns

### 1. Singleton Pattern

Used in utility classes to ensure single instance and global access.

#### AppLogger (ConcurrentHashMap-based Singleton)

**Implementation:**
- Uses `ConcurrentHashMap` for thread-safe lazy initialization
- Caches logger instances by class name or custom name
- Provides per-class logger instances

**Benefits:**
- Thread-safe without explicit synchronization
- Efficient caching reduces object creation overhead
- Memory efficient with lazy initialization

**Code Example:**
```java
private static final ConcurrentHashMap<String, AppLogger> LOGGER_CACHE = new ConcurrentHashMap<>();

public static AppLogger getInstance(Class<?> clazz) {
    String className = clazz.getName();
    return LOGGER_CACHE.computeIfAbsent(className, k -> new AppLogger(clazz));
}
```

#### DBConnection (Bill Pugh Singleton)

**Implementation:**
- Uses inner static class for lazy initialization
- Thread-safe without synchronization overhead
- Single HikariCP connection pool instance

**Benefits:**
- Lazy initialization (created only when needed)
- Thread-safe by JVM classloader mechanism
- No synchronization overhead
- Single connection pool for entire application

**Code Example:**
```java
private static class SingletonHelper {
    private static final DBConnection INSTANCE = new DBConnection();
}

public static DBConnection getInstance() {
    return SingletonHelper.INSTANCE;
}
```

### 2. Factory Pattern

**Spring Bean Factory:**
- Spring IoC container acts as a factory for managing beans
- DatabaseConfig creates and configures DataSource beans
- Conditional bean creation based on configuration

### 3. Dependency Injection

**Spring Framework DI:**
- Constructor injection for required dependencies
- Field injection for Spring-managed components
- Promotes loose coupling and testability

## Database Architecture

### PostgreSQL Cluster Setup

#### Primary-Replica Architecture

```
┌─────────────────┐
│   Application   │
└────────┬────────┘
         │
    ┌────┴─────┐
    │ PgBouncer│ (Connection Pooling)
    └────┬─────┘
         │
    ┌────┴──────────────────┐
    │   PostgreSQL Primary  │ (Port 5432)
    │   (Read/Write)        │
    └────┬──────────────────┘
         │
         ├─────────────────┐
         │                 │
    ┌────┴──────────┐ ┌────┴──────────┐
    │ Replica Node 1│ │ Replica Node 2│
    │ (Port 5433)   │ │ (Port 5434)   │
    │ (Read-Only)   │ │ (Read-Only)   │
    └───────────────┘ └───────────────┘
```

#### Replication Strategy

**Streaming Replication:**
- WAL (Write-Ahead Logging) streaming from primary to replicas
- Asynchronous replication (synchronous_commit=off)
- Hot standby mode on replicas

**Configuration Parameters:**
- `wal_level=replica` - Enables replication
- `max_wal_senders=10` - Maximum concurrent replication connections
- `max_replication_slots=10` - Maximum replication slots
- `hot_standby=on` - Allows read operations on replicas
- `synchronous_commit=off` - Enables eventual consistency

### CAP Theorem Trade-offs

Our configuration prioritizes **Availability** and **Partition Tolerance**:

- **Availability (A):** Multiple replica nodes ensure system remains operational
- **Partition Tolerance (P):** Asynchronous replication handles network partitions
- **Consistency (C):** Eventual consistency model (trades immediate consistency for availability)

**Use Case:** Ideal for read-heavy workloads where occasional stale reads are acceptable.

### Connection Pooling

#### HikariCP Configuration

**Pool Settings:**
- Maximum Pool Size: 10 connections
- Minimum Idle: 5 connections
- Connection Timeout: 30 seconds
- Idle Timeout: 10 minutes
- Max Lifetime: 30 minutes
- Leak Detection Threshold: 60 seconds

**Benefits:**
- Reduced connection overhead
- Better resource utilization
- Improved performance under load
- Connection leak detection

#### PgBouncer Configuration

**Pool Mode:** Transaction pooling
- Client connections: 1000
- Default pool size: 25
- Reserve pool: 5

**Benefits:**
- Additional connection pooling layer
- Load balancing across database nodes
- Connection multiplexing

## Logging Architecture

### Log4j2 Configuration

#### Appender Strategy

**Console Appender:**
- Synchronous logging for immediate feedback
- Threshold: INFO level and above
- Used during development and debugging

**Rolling File Appenders:**

1. **application.log** - All application logs
   - Daily rotation
   - 10MB size trigger
   - 30-day retention

2. **error.log** - Error logs only
   - Daily rotation
   - 10MB size trigger
   - 30-day retention

3. **database.log** - Database-specific logs
   - Daily rotation
   - 10MB size trigger
   - 15-day retention

#### Logger Hierarchy

```
Root Logger (INFO)
├── com.ratingsandreviews (DEBUG)
│   ├── util.DBConnection (INFO → database.log)
│   └── [other packages]
├── org.springframework (INFO)
├── org.hibernate (WARN)
└── com.zaxxer.hikari (INFO → database.log)
```

### SLF4J with Log4j2

**Benefits:**
- Facade pattern for logging (SLF4J)
- Pluggable logging implementation (Log4j2)
- Better performance than Logback
- Asynchronous logging support
- Zero-garbage logging

## Configuration Management

### Environment-based Configuration

**Application Properties:**
- `application.yml` - Main configuration
- `application-{profile}.yml` - Profile-specific config (dev, prod, test)

**Environment Variables:**
- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `DB_POOL_SIZE` - Connection pool size
- `SERVER_PORT` - Application server port

**Benefits:**
- 12-factor app compliance
- Environment-specific configurations
- Secure credential management

### Spring Profiles

**Test Profile:**
- H2 in-memory database
- Reduced logging
- Fast startup

**Production Profile:**
- PostgreSQL cluster
- Optimized settings
- Enhanced security

## Security Considerations

### Dependency Security

All dependencies use latest stable, secure versions:
- Spring Boot 3.2.1 (Dec 2023)
- PostgreSQL 16.1 (Nov 2023)
- Log4j2 2.22.1 (Nov 2023) - Addresses Log4Shell vulnerabilities
- PostgreSQL Driver 42.7.1 (Dec 2023)
- HikariCP 5.1.0 (Dec 2023)

### Database Security

**Replication Security:**
- SCRAM-SHA-256 authentication
- Network isolation via Docker bridge network
- Restricted replication user privileges

**Connection Security:**
- Connection pooling prevents connection exhaustion
- Prepared statement caching
- SQL injection protection via JPA/Hibernate

### Application Security

**Spring Security (Future Enhancement):**
- Authentication and authorization
- CSRF protection
- Security headers
- Rate limiting

## Testing Strategy

### Unit Testing

**Utility Tests:**
- AppLogger singleton behavior
- Thread-safety verification
- Method functionality

**Integration Tests:**
- Spring context loading
- Database connectivity
- Repository operations

### Test Isolation

**H2 In-Memory Database:**
- Fast test execution
- No external dependencies
- Clean state per test
- Conditional configuration

## Performance Considerations

### Database Performance

**Connection Pooling:**
- Pre-initialized connections
- Reduced connection overhead
- Optimal pool sizing

**Query Optimization:**
- Prepared statement caching
- Batch operations
- Indexed columns

**Read Scaling:**
- Read replicas for SELECT operations
- Write operations to primary only
- PgBouncer for load distribution

### Application Performance

**Logging Performance:**
- Asynchronous appenders (optional)
- Log level filtering
- Structured logging

**JVM Tuning:**
- Garbage collection optimization
- Heap sizing
- Thread pool configuration

## Scalability

### Horizontal Scaling

**Application Layer:**
- Stateless application design
- Multiple application instances
- Load balancer in front

**Database Layer:**
- Read replicas for read scaling
- Write operations always to primary
- Future: Sharding for write scaling

### Vertical Scaling

**Connection Pool Sizing:**
- Configurable via environment variables
- Based on available resources
- Monitor active vs idle connections

## Monitoring and Observability

### Logging

**Structured Logging:**
- Consistent log format
- Contextual information
- Correlation IDs (future)

**Log Aggregation:**
- Centralized log collection
- Log search and analysis
- Alerting on errors

### Health Checks

**Endpoints:**
- `/api/health` - Application health
- `/api/health/db` - Database health
- Connection pool metrics

### Metrics (Future Enhancement)

**Application Metrics:**
- Request rate and latency
- Error rates
- JVM metrics

**Database Metrics:**
- Connection pool usage
- Query performance
- Replication lag

## Future Enhancements

### Planned Improvements

1. **Spring Security Integration**
   - JWT-based authentication
   - Role-based access control
   - API rate limiting

2. **Caching Layer**
   - Redis for distributed caching
   - Application-level caching
   - Cache invalidation strategies

3. **Message Queue**
   - Kafka/RabbitMQ for async processing
   - Event-driven architecture
   - Eventual consistency handling

4. **Monitoring Stack**
   - Prometheus for metrics
   - Grafana for visualization
   - ELK stack for log analysis

5. **API Documentation**
   - OpenAPI/Swagger
   - Interactive API docs
   - API versioning

6. **Containerization**
   - Kubernetes deployment
   - Auto-scaling
   - Service mesh (Istio)

## Conclusion

This architecture provides:
- ✅ Scalable and maintainable codebase
- ✅ High availability with PostgreSQL clustering
- ✅ Comprehensive logging and monitoring
- ✅ Secure and up-to-date dependencies
- ✅ Testable and modular design
- ✅ Industry-standard design patterns
- ✅ Production-ready configuration

The system is built following best practices and is ready for both development and production deployment.
