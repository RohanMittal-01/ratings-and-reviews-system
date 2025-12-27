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

You can run the application in two ways:
1. **Using Docker Compose** (Recommended) - Runs everything in containers
2. **Local Development** - Run the application locally with Docker for database only

### Option 1: Docker Compose (Recommended)

This approach runs both the database cluster and the application in containers.

#### 1. Clone the Repository

```bash
git clone https://github.com/RohanMittal-01/ratings-and-reviews-system.git
cd ratings-and-reviews-system
```

#### 2. Start All Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL primary node on port 5432
- PostgreSQL replica-1 on port 5433
- PostgreSQL replica-2 on port 5434
- PgBouncer on port 6432
- Redis cache on port 6380
- **Ratings and Reviews Application on port 8080**

#### 3. Verify Services are Running

```bash
docker-compose ps
```

All services should show as "healthy" or "running".

#### 4. Test the Application

```bash
# Health check
curl http://localhost:8080/api/health

# Database health check
curl http://localhost:8080/api/health/db
```

#### 5. View Application Logs

```bash
docker-compose logs -f ratings-app
```

#### 6. Stop All Services

```bash
docker-compose down
```

To also remove volumes (database data):

```bash
docker-compose down -v
```

### Option 2: Local Development

Run the application locally for development while using Docker for the database.

#### 1. Clone the Repository

```bash
git clone https://github.com/RohanMittal-01/ratings-and-reviews-system.git
cd ratings-and-reviews-system
```

#### 2. Start PostgreSQL Cluster Only

To run only the database services without the application:

```bash
docker-compose up -d postgres-primary postgres-replica-1 postgres-replica-2 pgbouncer
```

Wait for all database services to be healthy:

```bash
docker-compose ps
```

#### 3. Build the Application

```bash
./gradlew clean build
```

#### 4. Run the Application Locally

```bash
./gradlew bootRun
```

Or run the JAR:

```bash
java -jar build/libs/ratings-and-reviews-system-1.0.0-SNAPSHOT.jar
```

The application will start on port 8080 by default.

## Configuration

### Environment Variables (Docker Compose)

When running with Docker Compose, you can customize the application using environment variables. Create a `.env` file in the project root or set them in your shell:

```bash
# Database connection
DB_URL=jdbc:postgresql://postgres-primary:5432/ratings_reviews
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Connection pool settings
DB_POOL_SIZE=10
DB_MIN_IDLE=5
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000

# Redis connection
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# Server configuration
SERVER_PORT=8080

# Spring profiles
SPRING_PROFILES_ACTIVE=default
```

Then start with:

```bash
docker-compose up -d
```

### Database Configuration (Local Development)

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

## API Documentation

### OpenAPI Specification

The complete API documentation is available in the `openapi.yaml` file at the project root. This file follows OpenAPI 3.0 standards and includes:

- All endpoint definitions
- Request/response schemas
- Parameter descriptions
- Example payloads
- Authentication requirements

You can view the API documentation using:
- [Swagger Editor](https://editor.swagger.io/) - Paste the contents of `openapi.yaml`
- [Swagger UI](https://swagger.io/tools/swagger-ui/) - Host the specification file
- Any OpenAPI-compatible tool

## API Endpoints

### Health Check

```bash
# Basic health check
curl http://localhost:8080/api/health

# Database health check
curl http://localhost:8080/api/health/db
```

### Applications

```bash
# Get all applications (paginated)
curl "http://localhost:8080/api/v1/applications?page=0&size=10"

# Get specific application
curl http://localhost:8080/api/v1/applications/{applicationId}

# Install an application
curl -X POST http://localhost:8080/api/v1/applications/install \
  -H "Content-Type: application/json" \
  -d '{"applicationId": "123e4567-e89b-12d3-a456-426614174000"}'
```

### Ratings

```bash
# Get ratings for an application
curl "http://localhost:8080/api/v1/ratings/{applicationId}?page=0&size=10"

# Get average rating
curl http://localhost:8080/api/v1/ratings/average/{applicationId}

# Submit a rating
curl -X POST http://localhost:8080/api/v1/ratings \
  -H "Content-Type: application/json" \
  -d '{"rating": 5, "applicationId": "123e4567-e89b-12d3-a456-426614174000", "userName": "john_doe"}'
```

### Comments/Reviews

```bash
# Get comments for an application
curl "http://localhost:8080/api/v1/comments/application/{applicationId}?page=0&size=10"

# Add a review (comment with no parent)
curl -X POST http://localhost:8080/api/v1/comments \
  -H "Content-Type: application/json" \
  -d '{"applicationId": "123e4567-e89b-12d3-a456-426614174000", "userId": "223e4567-e89b-12d3-a456-426614174001", "text": "Great app!", "sentiment": 1}'

# Update a comment
curl -X PUT http://localhost:8080/api/v1/comments/{commentId} \
  -H "Content-Type: application/json" \
  -d '{"text": "Updated comment", "sentiment": 1}'

# Delete a comment
curl -X DELETE "http://localhost:8080/api/v1/comments/{commentId}?applicationId={applicationId}"
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

### Full Stack (Database + Application)

```bash
# Start all services (database cluster + application)
docker-compose up -d

# Start with build (rebuild the application image)
docker-compose up -d --build

# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f ratings-app
docker-compose logs -f postgres-primary

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Restart the application service only
docker-compose restart ratings-app

# Rebuild and restart the application
docker-compose up -d --build ratings-app
```

### Database Only

```bash
# Start only database services
docker-compose up -d postgres-primary postgres-replica-1 postgres-replica-2 pgbouncer

# Stop only database services
docker-compose stop postgres-primary postgres-replica-1 postgres-replica-2 pgbouncer
```

### Docker Image Management

```bash
# Build the application Docker image manually
docker build -t ratings-and-reviews-system:latest .

# Run the application container manually
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ratings_reviews \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  --name ratings-app \
  ratings-and-reviews-system:latest

# View container logs
docker logs -f ratings-app

# Execute commands in the running container
docker exec -it ratings-app sh
```

### Database Access

```bash
# Access PostgreSQL primary
docker exec -it postgres-primary psql -U postgres -d ratings_reviews

# Access PostgreSQL replica-1
docker exec -it postgres-replica-1 psql -U postgres -d ratings_reviews

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
