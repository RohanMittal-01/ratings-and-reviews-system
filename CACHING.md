# Caching Implementation

## Overview
This application uses a two-tier caching strategy for optimal performance:
- **Caffeine (In-Memory)**: For user-specific, frequently accessed data
- **Redis (Distributed)**: For shared data across instances

## Architecture

### Cache Factory Pattern
- `CacheFactory`: Singleton factory to get cache instances at runtime
- `CacheService`: Common interface for all cache implementations
- `CaffeineCacheService`: In-memory LFU cache with 10,000 entry limit and 30-minute TTL
- `RedisCacheService`: Distributed cache with 1-hour default TTL

### Cache Key Strategy
Keys are structured hierarchically using `CacheKeyBuilder`:
- Comments: `comments:user:{userId}:app:{appId}:sentiment:{sentiment}:page:{page}:{size}`
- Ratings: `rating:avg:{appId}`, `rating:stats:{appId}`, `rating:page:{appId}:{page}:{size}`
- Applications: `application:{appId}`, `applications:{filterKey}:{filterValue}:{page}:{size}`

## Caching Decisions

### Caffeine (In-Memory) Cache
**Used for:**
- `getUserCommentsForApplication`: User-specific comment queries with context
- `getComments` (when userId present): User-first comment display
- `getCommentTree` (when userId present): User-specific tree view

**Why Caffeine:**
- User-specific data changes frequently per user
- Small data size (per-user queries)
- High read frequency
- LFU eviction suitable for hot user data
- 30-minute TTL balances freshness and performance

### Redis (Distributed) Cache
**Used for:**
- `getApplication`: Individual application details
- `getApplications`: Paginated application lists
- `getRatingForApplication`: Average ratings
- `getCategoryStatsForApplication`: Rating category stats
- `getRatingsByApplicationId`: Paginated ratings

**Why Redis:**
- Data shared across all users
- Distributed caching for multi-instance deployments
- 1-hour TTL (ratings/applications change infrequently)
- Supports pattern-based eviction for related keys

## Eviction Policies

### User Action-Driven Eviction
When a user performs an action, **only their cache** is evicted to ensure immediate visibility:

1. **Add Comment** (`addComment`):
   - Evict: `comments:user:{userId}:*`
   - Evict: `comments:app:{appId}:*:user:{userId}:*`
   - Evict: `comments:tree:{appId}:*`

2. **Update Comment** (`updateComment`):
   - Same as Add Comment

3. **Delete Comment** (`deleteComment`):
   - Same as Add Comment

4. **Submit Rating** (`submitRating`):
   - Evict: `rating:*:{appId}` (all rating caches for the application)
   - Evict average, stats, and paginated ratings

### Pattern-Based Eviction
- Caffeine: Prefix matching (e.g., `comments:user:123` evicts all keys starting with this)
- Redis: Pattern matching with `keys()` (e.g., `rating:*:appId` evicts all rating keys for appId)

## Configuration

### Caffeine Settings
- **Max Size**: 10,000 entries
- **TTL**: 30 minutes (expireAfterWrite)
- **Eviction Policy**: LFU (Least Frequently Used)
- **Stats Tracking**: Enabled for monitoring

### Redis Settings
- **Host**: `${REDIS_HOST:localhost}`
- **Port**: `${REDIS_PORT:6380}` (Using 6380 to avoid conflicts with local Redis)
- **Default TTL**: 3600 seconds (1 hour)
- **Connection Pool**: Lettuce with 10 max active connections
- **Timeout**: 3 seconds

## Usage Examples

### Getting a Cache Instance
```java
// Get Caffeine cache
CacheService caffeineCache = CacheFactory.getCaffeineCache();

// Get Redis cache
CacheService redisCache = CacheFactory.getRedisCache();
```

### Caching a Value
```java
String key = CacheKeyBuilder.userCommentsKey(appId, userId, sentiment, page, size);
UserCommentsResponse response = caffeineCache.get(key, UserCommentsResponse.class);
if (response == null) {
    response = fetchFromDatabase();
    caffeineCache.put(key, response);
}
```

### Evicting User-Specific Cache
```java
// Evict all cache entries for a user
caffeineCache.evictPattern(CacheKeyBuilder.userCommentsPattern(userId));
```

### Evicting Application-Wide Cache
```java
// Evict all rating caches for an application
redisCache.evictPattern(CacheKeyBuilder.ratingsPattern(applicationId));
```

## Performance Benefits

1. **Reduced Database Load**: Frequent reads served from cache
2. **Faster Response Times**: In-memory reads (~1ms) vs DB reads (~10-50ms)
3. **User-Centric UX**: User changes immediately visible (targeted eviction)
4. **Scalability**: Redis enables horizontal scaling across instances
5. **Cost Efficiency**: Reduced DB queries = lower infrastructure costs

## Monitoring

### Caffeine Stats
Access via `CaffeineCacheService.getInstance().getCache().stats()`:
- Hit rate
- Miss rate
- Eviction count
- Load success/failure count

### Redis Monitoring
Use Redis CLI or monitoring tools:
- `INFO stats` - Cache hit/miss rates
- `DBSIZE` - Total keys
- `KEYS pattern` - View cached keys (use sparingly in production)

## Deployment Requirements

1. **Redis Server**: 
   - **Option 1 - Docker Compose** (Recommended): Run `docker-compose up redis` to start Redis on port 6380
   - **Option 2 - Local Redis**: If running Redis locally on a different port, set `REDIS_PORT` environment variable
   
2. **Environment Variables**:
   - `REDIS_HOST`: Redis server hostname (default: localhost)
   - `REDIS_PORT`: Redis server port (default: 6380)
   - `REDIS_PASSWORD`: Redis password (if authentication enabled)

3. **Dependencies**: Run `./gradlew build` to download Caffeine and Redis dependencies

## Quick Start

```bash
# Start Redis using Docker Compose
docker-compose up -d redis

# Verify Redis is running
docker exec -it redis-cache redis-cli -p 6379 ping
# Should return: PONG

# Start your application
./gradlew bootRun
```

## Future Enhancements

1. **Cache Warming**: Pre-populate frequently accessed data on startup
2. **Multi-Level Eviction**: Cascade eviction to related entities
3. **TTL Tuning**: Dynamic TTL based on data access patterns
4. **Cache Metrics**: Expose cache stats via actuator endpoints
5. **Redis Sentinel/Cluster**: High availability setup for production

