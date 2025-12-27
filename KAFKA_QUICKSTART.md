# Quick Start Guide: Event-Driven Comments with Kafka

## Prerequisites
- Docker & Docker Compose
- Java 17+
- Gradle

## 1. Start Infrastructure

Start all required services (PostgreSQL, Redis, Kafka, Zookeeper):

```bash
cd /Users/kenkaneki/IdeaProjects/ratings-and-reviews-system

# Start all services
docker-compose up -d

# Verify all services are healthy
docker-compose ps
```

**Expected Output:**
```
NAME                STATUS              PORTS
postgres-primary    Up (healthy)        0.0.0.0:5432->5432/tcp
postgres-replica-1  Up (healthy)        0.0.0.0:5433->5432/tcp
postgres-replica-2  Up (healthy)        0.0.0.0:5434->5432/tcp
pgbouncer           Up (healthy)        0.0.0.0:6432->5432/tcp
redis-cache         Up (healthy)        0.0.0.0:6380->6379/tcp
zookeeper           Up (healthy)        0.0.0.0:2181->2181/tcp
kafka               Up (healthy)        0.0.0.0:9092-9093->9092-9093/tcp
```

## 2. Verify Kafka is Running

```bash
# List Kafka topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic (optional - auto-created by default)
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic comment-events \
  --partitions 3 \
  --replication-factor 1
```

## 3. Build and Run Application

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun
```

## 4. Test the System

### Add a Comment (Write to Cache → Kafka → DB)
```bash
curl -X POST "http://localhost:8080/api/v1/comments" \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "660e8400-e29b-41d4-a716-446655440000",
    "text": "This is a test comment!",
    "sentiment": 1,
    "parentId": null
  }'
```

**Expected Response (Immediate - from cache):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440000",
  "applicationId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "660e8400-e29b-41d4-a716-446655440000",
  "text": "This is a test comment!",
  "sentiment": 1,
  "level": 0,
  "createdAt": "2025-12-27T10:30:00Z"
}
```

### Monitor Kafka Events
```bash
# Watch events in real-time
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic comment-events \
  --from-beginning \
  --property print.key=true \
  --property print.timestamp=true
```

### Check Database (After ~1-2 seconds)
```bash
# Connect to PostgreSQL
docker exec -it postgres-primary psql -U postgres -d ratings_reviews

# Query comments
SELECT id, text, sentiment, user_id, created_at 
FROM ratings_reviews.comments 
ORDER BY created_at DESC 
LIMIT 5;
```

### Get Comments (Read from Cache)
```bash
curl "http://localhost:8080/api/v1/comments?applicationId=550e8400-e29b-41d4-a716-446655440000&page=0&size=10&userId=660e8400-e29b-41d4-a716-446655440000"
```

## 5. Monitor System Health

### Check Consumer Lag
```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group comment-persistence-group
```

**Expected Output:**
```
GROUP                      TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
comment-persistence-group  comment-events  0          150             150             0
comment-persistence-group  comment-events  1          145             145             0
comment-persistence-group  comment-events  2          148             148             0
```

**LAG = 0** means consumers are caught up (good!)  
**LAG > 0** means consumers are behind (check logs)

### Check Application Logs
```bash
tail -f logs/application.log | grep -E "Published event|Received event"
```

**Expected Output:**
```
[INFO] Published event: CREATED for comment: 770e8400-e29b-41d4-a716-446655440000
[INFO] Received event: CREATED for comment: 770e8400-e29b-41d4-a716-446655440000
[INFO] Successfully processed event: CREATED for comment: 770e8400-e29b-41d4-a716-446655440000
```

## 6. Performance Testing

### Load Test with Many Concurrent Writes
```bash
# Install Apache Bench (if not installed)
brew install httpd

# 1000 requests, 100 concurrent
ab -n 1000 -c 100 -p comment.json -T application/json \
  http://localhost:8080/api/v1/comments
```

**Where `comment.json` contains:**
```json
{
  "applicationId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "660e8400-e29b-41d4-a716-446655440000",
  "text": "Load test comment",
  "sentiment": 1,
  "parentId": null
}
```

**Expected Performance:**
- **Response Time**: 1-5ms (cache write only)
- **Throughput**: 10,000-50,000 requests/sec
- **Consumer Lag**: Temporarily increases, then catches up

## 7. Troubleshooting

### Issue: Kafka Connection Refused
```bash
# Check if Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka

# Restart Kafka
docker-compose restart kafka
```

### Issue: Consumer Not Processing Events
```bash
# Check consumer group
docker exec -it kafka kafka-consumer-groups --list --bootstrap-server localhost:9092

# Reset consumer offset (if stuck)
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group comment-persistence-group \
  --reset-offsets --to-earliest --execute --topic comment-events
```

### Issue: Database Not Updated
```bash
# Check application logs for errors
tail -f logs/application.log | grep ERROR

# Manually trigger consumer
# (Consumer should auto-retry on failure)
```

### Issue: Cache Not Working
```bash
# Check Redis
docker exec -it redis-cache redis-cli ping

# Check Caffeine stats
# (Add logging in CaffeineCacheService)
```

## 8. Architecture Validation

### Verify Strong Consistency for User
```bash
# 1. Add comment
COMMENT_ID=$(curl -s -X POST "http://localhost:8080/api/v1/comments" \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "660e8400-e29b-41d4-a716-446655440000",
    "text": "Test immediate visibility",
    "sentiment": 1
  }' | jq -r '.id')

# 2. Immediately fetch comments (should include new comment)
curl "http://localhost:8080/api/v1/comments?applicationId=550e8400-e29b-41d4-a716-446655440000&userId=660e8400-e29b-41d4-a716-446655440000" | jq '.content[] | select(.id=="'$COMMENT_ID'")'
```

**Expected**: Comment visible immediately in response ✅

### Verify Eventual Consistency for Database
```bash
# Wait 2 seconds, then check DB
sleep 2
docker exec -it postgres-primary psql -U postgres -d ratings_reviews -c \
  "SELECT id, text FROM ratings_reviews.comments WHERE id='$COMMENT_ID';"
```

**Expected**: Comment present in database after ~1-2 seconds ✅

## 9. Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v
```

## 10. Production Deployment

### Environment Variables
```bash
export KAFKA_BOOTSTRAP_SERVERS=kafka-cluster:9092
export REDIS_HOST=redis-cluster
export REDIS_PORT=6379
export POSTGRES_HOST=postgres-primary
export POSTGRES_PORT=5432
```

### Scale Consumers
Update `docker-compose.yml` or Kubernetes deployment:
```yaml
replicas: 3  # 3 instances = 9 consumer threads (3 per instance)
```

### Monitor in Production
- **Kafka**: Use Confluent Control Center or Burrow
- **Redis**: Use Redis Sentinel or Redis Cluster
- **PostgreSQL**: Use pgAdmin or Datadog
- **Application**: Use Prometheus + Grafana

## Summary

✅ **Blazing Fast Writes**: 1-5ms response time (cache only)  
✅ **Strong Consistency for User**: User sees changes immediately  
✅ **Eventual Consistency for DB**: Database updated within seconds  
✅ **Highly Scalable**: Kafka handles millions of events/sec  
✅ **Fault Tolerant**: Events persisted in Kafka, retries on failure  

You now have a **production-grade event-driven architecture** for comments! 🚀

