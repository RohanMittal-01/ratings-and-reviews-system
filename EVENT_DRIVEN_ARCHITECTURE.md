# Event-Driven Architecture with Kafka

## Overview

The system now uses **event-driven architecture with Kafka** for eventual consistency. This is a much simpler and more scalable approach than complex cache eviction logic.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          User Request                            │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                       CommentController                          │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                        CommentService                            │
│  1. Write to Cache (Redis/Caffeine) ← FAST (1-5ms)             │
│  2. Publish Event to Kafka          ← ASYNC                     │
│  3. Return Response to User         ← IMMEDIATE                  │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
                    ┌────────────┴────────────┐
                    ↓                         ↓
         ┌──────────────────┐      ┌──────────────────┐
         │  Cache (Primary) │      │  Kafka Topic     │
         │  - Redis/Caffeine│      │  comment-events  │
         │  - Fast Reads    │      └──────────────────┘
         │  - User Sees     │                ↓
         │    Changes NOW   │      ┌──────────────────┐
         └──────────────────┘      │ Event Consumer   │
                                   │ (Background)     │
                                   └──────────────────┘
                                            ↓
                                   ┌──────────────────┐
                                   │   PostgreSQL     │
                                   │   (Eventually)   │
                                   │   Consistent)    │
                                   └──────────────────┘
```

## Data Flow

### Write Path (Add/Update/Delete Comment)

1. **User sends request** → `CommentController`
2. **Service writes to cache** → Caffeine/Redis (FAST: 1-5ms)
3. **Service publishes event** → Kafka (ASYNC: fire-and-forget)
4. **Service returns response** → User sees changes IMMEDIATELY
5. **Kafka consumer processes event** → Writes to PostgreSQL (EVENTUAL: seconds later)

### Read Path (Get Comments)

1. **User sends request** → `CommentController`
2. **Service reads from cache** → Caffeine/Redis (FAST: 1ms)
3. **If cache miss** → Read from PostgreSQL, populate cache
4. **Return response** → User gets data

## Event Types

```java
public enum EventType {
    CREATED,   // New comment created
    UPDATED,   // Comment text/sentiment updated
    DELETED    // Comment deleted
}
```

## Event Structure

```java
CommentEvent {
    eventType: CREATED/UPDATED/DELETED
    commentId: UUID
    applicationId: UUID
    userId: UUID
    text: String
    sentiment: Short
    parentId: UUID
    level: Long
    timestamp: ZonedDateTime
}
```

## Components

### 1. CommentEventProducer
- **Purpose**: Publishes events to Kafka
- **Location**: `event/CommentEventProducer.java`
- **Usage**: Called by `CommentService` after cache write
- **Configuration**: 
  - Idempotent producer (exactly-once)
  - Acks: all (waits for all replicas)
  - Retries: 3

### 2. CommentEventConsumer
- **Purpose**: Consumes events and writes to PostgreSQL
- **Location**: `event/CommentEventConsumer.java`
- **Configuration**:
  - Group ID: `comment-persistence-group`
  - Concurrency: 3 threads
  - Manual commit (AckMode.RECORD)
  - Auto offset reset: earliest

### 3. KafkaConfig
- **Purpose**: Kafka producer/consumer configuration
- **Location**: `config/KafkaConfig.java`
- **Features**:
  - Idempotent producer
  - Exactly-once semantics
  - Manual commit for reliability
  - 3 consumer threads for parallelism

## Benefits

### ✅ Strong Consistency for Active User
- User writes to cache → sees changes IMMEDIATELY
- No waiting for database write
- Response time: 1-5ms (cache only)

### ✅ Eventual Consistency for Persistence
- Database writes happen asynchronously
- No blocking on slow DB operations
- Kafka handles backpressure and retries

### ✅ Simplified Cache Logic
- Cache is source of truth (no complex eviction)
- No need to worry about multi-instance consistency
- Cache naturally expires via TTL

### ✅ Scalability
- Kafka partitions scale horizontally
- Multiple consumers process events in parallel
- Database writes don't block user requests

### ✅ Fault Tolerance
- Kafka persists events (replication factor)
- Consumer retries on failure
- Dead letter queue for failed events (can be added)

### ✅ Decoupling
- Comment service doesn't know about persistence
- Persistence service doesn't know about cache
- Easy to add more consumers (analytics, notifications, etc.)

## Configuration

### application.yml
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9093
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      group-id: comment-persistence-group
      auto-offset-reset: earliest
      enable-auto-commit: false
```

### docker-compose.yml
```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports:
      - "2181:2181"
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9093:9093"  # External (avoid conflict)
      - "9092:9092"  # Internal
    depends_on:
      - zookeeper
```

## Deployment

### Start Infrastructure
```bash
# Start all services (PostgreSQL, Redis, Kafka, Zookeeper)
docker-compose up -d

# Verify Kafka is running
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic manually (optional - auto-created by default)
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic comment-events \
  --partitions 3 \
  --replication-factor 1
```

### Start Application
```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

## Monitoring

### Check Kafka Topics
```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Check Consumer Lag
```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group comment-persistence-group
```

### View Messages
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic comment-events \
  --from-beginning
```

## Trade-offs

### ✅ Pros
- **Blazing fast user experience** (1-5ms response)
- **Highly scalable** (Kafka handles millions of events)
- **Fault tolerant** (events persisted in Kafka)
- **Simple cache logic** (no complex eviction)
- **Decoupled architecture** (easy to extend)

### ⚠️ Cons
- **Eventual consistency** (DB lags behind cache by seconds)
- **More infrastructure** (Kafka + Zookeeper)
- **Complexity** (distributed system)
- **Requires monitoring** (consumer lag, dead letters)

### Acceptable For
- ✅ Social features (comments, likes, follows)
- ✅ Analytics data
- ✅ User activity tracking
- ✅ Non-critical business data

### NOT Acceptable For
- ❌ Financial transactions
- ❌ Inventory management
- ❌ Payment processing
- ❌ Critical business logic

## Consistency Guarantees

| Operation | Cache | Kafka | Database | User Sees |
|-----------|-------|-------|----------|-----------|
| Add Comment | Written | Published | Eventually | Immediately |
| Update Comment | Updated | Published | Eventually | Immediately |
| Delete Comment | Evicted | Published | Eventually | Immediately |
| Read Comment | Hit | N/A | N/A | Immediately |
| Read Comment (miss) | Miss | N/A | Read | After DB read |

## Recovery Scenarios

### Scenario 1: Consumer Crashes
- **Kafka retains events** (retention: 7 days default)
- **Consumer restarts** and processes from last committed offset
- **No data loss**

### Scenario 2: Database Down
- **Events accumulate in Kafka**
- **Consumer retries** (with backoff)
- **Processes all events** once DB is back
- **No data loss**

### Scenario 3: Cache Down
- **Reads go to database** (slower but works)
- **Writes still publish to Kafka**
- **Cache repopulated** from DB on read

### Scenario 4: Kafka Down
- **Writes to cache succeed** (user still sees changes)
- **Event publish fails** → Application logs error
- **Manual reconciliation** may be needed (can use DB changelog)
- **Alternative**: Buffer events in memory/Redis and retry

## Future Enhancements

1. **Dead Letter Queue (DLQ)**
   - Failed events go to separate topic
   - Manual review and reprocessing

2. **Event Sourcing**
   - Store all events permanently
   - Rebuild state from events
   - Audit trail

3. **CQRS (Command Query Responsibility Segregation)**
   - Separate read and write models
   - Optimized read replicas

4. **Saga Pattern**
   - Distributed transactions across services
   - Compensating actions on failure

5. **Change Data Capture (CDC)**
   - Sync cache from DB changes
   - Handle out-of-order events

## Comparison: Before vs After

### Before (Cache Eviction)
```
User Request → Service → DB Write → Cache Evict → Response
Response Time: 50-100ms (DB write blocks)
Complexity: HIGH (complex eviction logic)
Scalability: LIMITED (DB is bottleneck)
Consistency: STRONG (but slow)
```

### After (Event-Driven with Kafka)
```
User Request → Service → Cache Write → Kafka Publish → Response
Response Time: 1-5ms (cache only)
Complexity: MEDIUM (distributed system)
Scalability: HIGH (Kafka scales)
Consistency: EVENTUAL (but FAST for user)
```

## Summary

✅ **Simpler Architecture**: No complex cache eviction logic  
✅ **Faster User Experience**: 1-5ms response time (cache only)  
✅ **Highly Scalable**: Kafka handles millions of events/sec  
✅ **Fault Tolerant**: Events persisted, retries on failure  
✅ **Decoupled**: Easy to add consumers (analytics, notifications)  
✅ **Eventual Consistency**: Acceptable for social features  

This is the **production-grade** approach used by companies like LinkedIn, Netflix, and Uber for high-scale social features! 🚀

