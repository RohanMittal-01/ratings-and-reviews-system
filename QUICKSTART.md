# Quick Start Guide

This guide will help you get the Ratings and Reviews System up and running in minutes.

## Prerequisites

Ensure you have the following installed:
- Java 17 or higher
- Docker and Docker Compose
- Git

## Step 1: Clone the Repository

```bash
git clone https://github.com/RohanMittal-01/ratings-and-reviews-system.git
cd ratings-and-reviews-system
```

## Step 2: Start the PostgreSQL Cluster

```bash
docker-compose up -d
```

Wait for all services to be healthy (about 30-60 seconds):

```bash
docker-compose ps
```

You should see all services with status "Up (healthy)".

## Step 3: Verify Database Cluster

Check that the primary node is running:

```bash
docker exec -it postgres-primary psql -U postgres -d ratings_reviews -c "SELECT version();"
```

Check replication status:

```bash
docker exec -it postgres-primary psql -U postgres -c "SELECT * FROM pg_stat_replication;"
```

## Step 4: Build the Application

```bash
./gradlew clean build
```

This will:
- Download all dependencies
- Compile the code
- Run all tests
- Create executable JAR

## Step 5: Run the Application

```bash
./gradlew bootRun
```

Or run the JAR directly:

```bash
java -jar build/libs/ratings-and-reviews-system-1.0.0-SNAPSHOT.jar
```

The application will start on port 8080.

## Step 6: Test the Application

### Basic Health Check

```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "application": "Ratings and Reviews System",
  "version": "1.0.0"
}
```

### Database Health Check

```bash
curl http://localhost:8080/api/health/db
```

Expected response:
```json
{
  "status": "UP",
  "database": "PostgreSQL",
  "totalConnections": 5,
  "activeConnections": 1,
  "idleConnections": 4
}
```

## Common Tasks

### View Application Logs

```bash
tail -f logs/application.log
```

### View Error Logs

```bash
tail -f logs/error.log
```

### View Database Logs

```bash
tail -f logs/database.log
```

### Access PostgreSQL Primary

```bash
docker exec -it postgres-primary psql -U postgres -d ratings_reviews
```

### Access PostgreSQL Replica

```bash
docker exec -it postgres-replica-1 psql -U postgres -d ratings_reviews
```

### View Docker Logs

```bash
# Primary node
docker-compose logs -f postgres-primary

# Replica 1
docker-compose logs -f postgres-replica-1

# All services
docker-compose logs -f
```

### Stop the Application

Press `Ctrl+C` to stop the application.

### Stop the Database Cluster

```bash
docker-compose down
```

To stop and remove volumes (clears all data):

```bash
docker-compose down -v
```

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test

```bash
./gradlew test --tests AppLoggerTest
```

### View Test Report

```bash
open build/reports/tests/test/index.html
```

## Configuration

### Change Database Connection

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ratings_reviews
    username: postgres
    password: postgres
```

Or use environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ratings_reviews
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
./gradlew bootRun
```

### Change Server Port

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 9090
```

Or use environment variable:

```bash
export SERVER_PORT=9090
./gradlew bootRun
```

### Change Log Level

Edit `src/main/resources/application.yml`:

```yaml
logging:
  level:
    com.ratingsandreviews: DEBUG
    org.springframework: INFO
```

## Development Workflow

### 1. Make Code Changes

Edit files in `src/main/java/com/ratingsandreviews/`

### 2. Build and Test

```bash
./gradlew clean build
```

### 3. Run Application

```bash
./gradlew bootRun
```

### 4. Test Endpoints

```bash
curl http://localhost:8080/api/health
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use:

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change application port
export SERVER_PORT=9090
./gradlew bootRun
```

### Database Connection Failed

Check if Docker containers are running:

```bash
docker-compose ps
```

Restart the cluster:

```bash
docker-compose down
docker-compose up -d
```

### Build Failed

Clean and rebuild:

```bash
./gradlew clean build --refresh-dependencies
```

### Tests Failed

Run tests with verbose output:

```bash
./gradlew test --info
```

View test report for details:

```bash
open build/reports/tests/test/index.html
```

## IDE Setup

### IntelliJ IDEA

1. Open the project directory in IntelliJ IDEA
2. IntelliJ will automatically detect Gradle and import the project
3. Enable annotation processing: `Settings → Build → Compiler → Annotation Processors`
4. Right-click `RatingsAndReviewsApplication.java` and select "Run"

### Eclipse

1. Install Buildship Gradle plugin
2. Import project: `File → Import → Gradle → Existing Gradle Project`
3. Select the project directory
4. Right-click the project and select `Gradle → Refresh Gradle Project`

### VS Code

1. Install Java Extension Pack
2. Install Gradle for Java extension
3. Open the project directory
4. Use Command Palette (`Ctrl+Shift+P`) and run "Gradle: Refresh Gradle Project"

## Next Steps

Now that you have the application running, you can:

1. Explore the code structure in `src/main/java/com/ratingsandreviews/`
2. Read the [ARCHITECTURE.md](ARCHITECTURE.md) for design details
3. Add new controllers in `controller/` package
4. Add new services in `service/` package
5. Add new entities in `model/` package
6. Write tests in `src/test/java/`

## Getting Help

- Read the [README.md](README.md) for detailed documentation
- Read the [ARCHITECTURE.md](ARCHITECTURE.md) for architecture details
- Check Docker logs: `docker-compose logs -f`
- Check application logs: `tail -f logs/application.log`
- Create an issue in the GitHub repository

Happy coding! 🚀
