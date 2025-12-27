# Redis Setup Guide

## Quick Start with Docker Compose

The easiest way to get Redis running for development is using the included docker-compose configuration:

```bash
# Start only Redis
docker-compose up -d redis

# Verify Redis is running
docker exec -it redis-cache redis-cli ping
# Expected output: PONG

# View Redis logs
docker-compose logs -f redis

# Stop Redis
docker-compose stop redis

# Remove Redis (including data)
docker-compose down -v redis
```

## Port Configuration

- **External Port**: 6380 (to avoid conflicts with local Redis installations)
- **Internal Port**: 6379 (default Redis port inside container)
- The application connects to `localhost:6380` by default

## Why Port 6380?

Many developers run Redis locally for other projects on the default port 6379. Using port 6380 for this project's Docker Redis instance prevents port conflicts and allows both to run simultaneously.

## Configuration

The Redis configuration is in two places:

1. **docker-compose.yml**: Container and port mapping
   ```yaml
   redis:
     ports:
       - "6380:6379"  # External:Internal
   ```

2. **application.yml**: Application connection settings
   ```yaml
   spring:
     data:
       redis:
         port: ${REDIS_PORT:6380}
   ```

## Using a Different Port

If you need to use a different port:

1. Update `docker-compose.yml`:
   ```yaml
   ports:
     - "YOUR_PORT:6379"
   ```

2. Set environment variable when running the application:
   ```bash
   REDIS_PORT=YOUR_PORT ./gradlew bootRun
   ```

## Redis Features

- **Persistence**: AOF (Append Only File) enabled for data durability
- **Health Check**: Automatic health monitoring
- **Data Volume**: Persistent storage in named volume `redis-data`

## Testing Redis Connection

```bash
# Connect to Redis CLI
docker exec -it redis-cache redis-cli

# Inside Redis CLI:
> PING
PONG
> SET test "Hello Redis"
OK
> GET test
"Hello Redis"
> exit
```

## Production Considerations

For production deployments:

1. **Enable Authentication**: Set a strong password
   ```yaml
   command: redis-server --appendonly yes --requirepass "strong_password_here"
   ```

2. **Use Redis Sentinel or Cluster**: For high availability
3. **Configure Persistence**: Tune AOF/RDB settings based on your needs
4. **Monitor Performance**: Use Redis monitoring tools
5. **Network Security**: Restrict access via firewall rules

## Troubleshooting

### Port Already in Use
```bash
# Check if port 6380 is in use
lsof -i :6380

# If needed, change the port in docker-compose.yml
```

### Connection Refused
```bash
# Check if Redis container is running
docker ps | grep redis

# Check Redis logs
docker-compose logs redis

# Restart Redis
docker-compose restart redis
```

### Clear Redis Cache
```bash
# Connect to Redis and flush all data
docker exec -it redis-cache redis-cli FLUSHALL
```

