# Ratings and Reviews System

A PlayStore-like Application system where users can add ratings and reviews/comments to applications.

## Features

- **Spring Boot 3.2.1** - Latest stable version with Java 17
- **Modular Architecture** - Clean separation of concerns (config, util, controller, service, repository, model)
- **PostgreSQL Cluster** - High availability setup with primary-replica architecture
- **Connection Pooling** - HikariCP for optimal database performance
- **Logging** - Log4j2 with SLF4J for comprehensive logging
- **Design Patterns** - Singleton pattern for utilities (DBConnection, AppLogger)
- **Docker Compose** - Easy deployment with PostgreSQL cluster

## Architecture

### Package Structure

```
com.ratingsandreviews/
├── config/          # Configuration classes
│   └── DatabaseConfig.java
├── controller/      # REST controllers
│   └── HealthController.java
├── service/         # Business logic layer
├── repository/      # Data access layer
├── model/           # Domain models/entities
├── util/            # Utility classes
│   ├── AppLogger.java      # Singleton logger utility
│   └── DBConnection.java   # Singleton DB connection utility
└── RatingsAndReviewsApplication.java  # Main application class
```

### Database Cluster

The PostgreSQL cluster consists of:
- **Primary Node** (Port 5432): Handles all write operations
- **Replica Node 1** (Port 5433): Read-only replica for load distribution
- **Replica Node 2** (Port 5434): Additional replica for high availability
- **PgBouncer** (Port 6432): Connection pooler for load balancing

This setup provides:
- **Availability**: Multiple nodes ensure system remains operational
- **Eventual Consistency**: Asynchronous replication (synchronous_commit=off)
- **Read Scaling**: Read operations can be distributed across replicas
- **Failover Support**: Replicas can be promoted to primary if needed

## Prerequisites

- Java 17 or higher
- Gradle 8.5+ (wrapper included)
- Docker and Docker Compose
- Git

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/RohanMittal-01/ratings-and-reviews-system.git
cd ratings-and-reviews-system
```

### 2. Start PostgreSQL Cluster

```bash
docker-compose up -d
```

This will start:
- PostgreSQL primary node on port 5432
- PostgreSQL replica-1 on port 5433
- PostgreSQL replica-2 on port 5434
- PgBouncer on port 6432

Wait for all services to be healthy:

```bash
docker-compose ps
```

### 3. Build the Application

```bash
./gradlew clean build
```

### 4. Run the Application

```bash
./gradlew bootRun
```

Or run the JAR:

```bash
java -jar build/libs/ratings-and-reviews-system-1.0.0-SNAPSHOT.jar
```

The application will start on port 8080 by default.

## Configuration

### Database Configuration

You can configure the database connection using environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ratings_reviews
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DB_POOL_SIZE=10
export DB_MIN_IDLE=5
export DB_CONNECTION_TIMEOUT=30000
export DB_IDLE_TIMEOUT=600000
export DB_MAX_LIFETIME=1800000
```

### Application Configuration

Edit `src/main/resources/application.yml` to customize:
- Server port
- Database connection settings
- Logging levels
- JPA/Hibernate settings

## API Endpoints

### Health Check

```bash
# Basic health check
curl http://localhost:8080/api/health

# Database health check
curl http://localhost:8080/api/health/db
```

## Utility Classes

### AppLogger (Singleton)

Thread-safe logger utility using SLF4J with Log4j2:

```java
private static final AppLogger logger = AppLogger.getInstance(YourClass.class);

logger.info("Information message");
logger.debug("Debug message with param: {}", value);
logger.error("Error message", exception);
```

### DBConnection (Singleton)

Thread-safe database connection utility:

```java
DBConnection dbConnection = DBConnection.getInstance();
Connection conn = dbConnection.getConnection();

// Check connection pool status
int total = dbConnection.getTotalConnections();
int active = dbConnection.getActiveConnections();
int idle = dbConnection.getIdleConnections();
```

## Design Patterns

### Singleton Pattern

Both utility classes implement the Singleton pattern using different approaches:

1. **AppLogger**: Uses ConcurrentHashMap for lazy initialization with caching
2. **DBConnection**: Uses Bill Pugh Singleton (inner static class) for lazy initialization

Benefits:
- Single instance ensures consistent behavior
- Thread-safe implementation
- Resource efficiency (single connection pool, cached loggers)
- Global access point

## Logging

Logs are written to:
- `logs/application.log` - All application logs
- `logs/error.log` - Error logs only
- `logs/database.log` - Database-related logs
- Console output for INFO and above

Log rotation:
- Daily rotation
- Maximum file size: 10MB
- Retention: 30 days (15 days for database logs)

## Docker Commands

```bash
# Start the cluster
docker-compose up -d

# Stop the cluster
docker-compose down

# View logs
docker-compose logs -f postgres-primary
docker-compose logs -f postgres-replica-1

# Access PostgreSQL
docker exec -it postgres-primary psql -U postgres -d ratings_reviews

# Check replication status
docker exec -it postgres-primary psql -U postgres -c "SELECT * FROM pg_stat_replication;"
```

## Database Schema

The initial schema includes:
- `applications` - Application information
- `users` - User accounts
- `ratings` - User ratings for applications
- `reviews` - User reviews/comments for applications

## Security

- Latest stable versions of all dependencies
- PostgreSQL 16.1 (latest stable)
- Spring Boot 3.2.1
- Log4j2 2.22.1 (addresses all known vulnerabilities)
- PostgreSQL driver 42.7.1
- HikariCP 5.1.0

## Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## Building for Production

```bash
# Build JAR
./gradlew clean bootJar

# Run in production mode
java -jar build/libs/ratings-and-reviews-system-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## TODO List (Future Scope)
- [ ] Implement user authentication and authorization
- [ ] Add an Event driven Architecture (EDA) for better scalability (already present in beta branch)
- [ ] Comments updated using Kafka messaging (already present in beta branch)
- [ ] Provide Documentation using Swagger/OpenAPI (copilot PR open)
- [ ] Add more unit and integration tests
- [ ] Benchmark performance under load using JMeter

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please open an issue in the GitHub repository.
