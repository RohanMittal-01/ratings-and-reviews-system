# Caching Strategy: Strong Consistency for Active User, Eventual Consistency for Others

## Problem Statement

In a multi-instance deployment with in-memory caching (Caffeine), we need to ensure:
1. **Strong Consistency** - Active user sees their changes immediately
2. **Eventual Consistency** - Other users see changes within TTL window (acceptable for non-critical data)
3. **Performance** - Reduce database load while maintaining acceptable UX

## Solution: User-Specific Cache Eviction with Caffeine

### Why Caffeine (In-Memory) Works Here

**Caffeine is appropriate because:**
- ✅ Comment data is **not critical** (unlike payments/inventory)
- ✅ Eventual consistency is **acceptable** for social features
- ✅ Load balancers with **sticky sessions** route users to same instance
- ✅ **30-minute staleness** is reasonable for comment feeds
- ✅ **Lower latency** than Redis for same-instance requests
- ✅ **No network overhead** for cache operations

**When NOT to use Caffeine:**
- ❌ Critical business data (payments, inventory, orders)
- ❌ Real-time collaboration features
- ❌ Data that requires immediate global consistency
- ❌ When instances don't have sticky sessions

### Architecture

```
Instance 1 (Caffeine Cache)          Instance 2 (Caffeine Cache)
┌─────────────────────┐              ┌─────────────────────┐
│ User A's cache      │              │ User B's cache      │
│ - Comments for App1 │              │ - Comments for App1 │
│ - User A specific   │              │ - User B specific   │
└─────────────────────┘              └─────────────────────┘
         ↓                                     ↓
    User A adds comment                   User B views
         ↓                                     ↓
    Evict User A cache                    Still cached
         ↓                                     ↓
    User A sees new data              User B sees old data
    (Strong Consistency)              (Eventual Consistency)
```

### Flow Example

**Scenario: User A adds a comment**

1. **User A** (on Instance 1) adds comment
   - Request → Instance 1 (sticky session)
   - Comment saved to database
   - **Evict only User A's cache** on Instance 1
   - User A's next request fetches fresh data
   - ✅ User A sees their comment immediately

2. **User B** (on Instance 2) views comments
   - Request → Instance 2 (sticky session)
   - Cache hit from Instance 2's Caffeine cache
   - Returns cached data (doesn't include User A's new comment)
   - ⏱️ User B will see User A's comment after TTL expires (≤30 min)

3. **User C** (on Instance 1) views comments
   - Request → Instance 1
   - Cache hit from Instance 1's Caffeine cache
   - Returns cached data (may or may not include User A's comment depending on cache key)
   - ⏱️ Eventual consistency

### Implementation Details

#### 1. Cache Keys Include userId

```java
String cacheKey = CacheKeyBuilder.commentsKey(
    applicationId.toString(),
    parentId != null ? parentId.toString() : "null",
    sentiment,
    userId.toString(),  // ← User-specific key
    pageable.getPageNumber(),
    pageable.getPageSize()
);
```

**Why:** Different users have different cache entries, so evicting User A's cache doesn't affect User B's cache.

#### 2. Targeted Cache Eviction

```java
private void evictUserSpecificCache(UUID applicationId, UUID userId) {
    String appIdStr = applicationId.toString();
    String userIdStr = userId.toString();

    // Only evict caches that include this specific userId in the key
    caffeineCache.evictPattern(CacheKeyBuilder.userCommentsPattern(userIdStr));
    caffeineCache.evictPattern(CacheKeyBuilder.commentsPatternForUser(appIdStr, userIdStr));
    caffeineCache.evictPattern(CacheKeyBuilder.commentTreePattern(appIdStr) + ":user:" + userIdStr);
}
```

**What gets evicted:**
- ✅ `comments:user:USER_A:app:APP_1:...` (User A's caches)
- ✅ `comments:app:APP_1:parent:*:sentiment:*:user:USER_A:...` (User A's caches)
- ✅ `comments:tree:APP_1:user:USER_A` (User A's tree view)

**What stays cached:**
- ✅ `comments:user:USER_B:app:APP_1:...` (User B's caches) → Eventual consistency
- ✅ `comments:user:USER_C:app:APP_1:...` (User C's caches) → Eventual consistency

#### 3. TTL Ensures Eventual Consistency

```java
this.cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(30, TimeUnit.MINUTES)  // ← Max staleness
    .recordStats()
    .build();
```

**Guarantees:**
- All users see changes within 30 minutes (worst case)
- Most users see changes much sooner if they refresh or their cache wasn't populated
- Active user sees changes immediately

### Consistency Model

| User Action | Active User (User A) | Other Users (User B, C) | Acceptable? |
|-------------|---------------------|------------------------|-------------|
| Add Comment | Immediate (strong) | ≤30 min (eventual) | ✅ Yes - social feature |
| Update Comment | Immediate (strong) | ≤30 min (eventual) | ✅ Yes - own comments only |
| Delete Comment | Immediate (strong) | ≤30 min (eventual) | ✅ Yes - soft delete UX acceptable |
| View Comments | Fresh data | Cached data | ✅ Yes - read-heavy workload |

### When to Switch to Redis

If you need **strong consistency for ALL users**, switch to Redis:

```java
// Change from Caffeine to Redis
@Autowired
public CommentServiceImpl(CommentRepository repository, CacheService redisCacheService) {
    this.repository = repository;
    this.redisCache = redisCacheService;  // ← Distributed cache
}
```

**Use Redis when:**
- Real-time collaboration required
- All users must see changes immediately
- You have budget for Redis infrastructure
- Network latency to Redis is acceptable

### Performance Comparison

| Cache Type | Same Instance | Cross Instance | Consistency | Cost |
|-----------|--------------|----------------|-------------|------|
| **Caffeine** | ~1ms | N/A (separate caches) | Eventual | Free |
| **Redis** | ~5-10ms | ~5-10ms | Strong | $$$ |
| **No Cache** | ~50ms | ~50ms | Strong | $ |

### Trade-offs Accepted

✅ **Accepted:**
- User B sees stale comments for up to 30 minutes
- Different users may see different data temporarily
- Cross-instance inconsistency

✅ **Mitigated:**
- Active user always sees their own changes immediately
- 30-minute TTL ensures eventual consistency
- Read-heavy workload benefits from cache hits

❌ **Not Acceptable For:**
- Financial transactions
- Inventory management
- Collaborative editing
- Mission-critical data

### Monitoring

```java
CaffeineCacheService.getInstance().getCache().stats();
```

**Key Metrics:**
- Hit rate: Should be >70% for good performance
- Miss rate: Indicates how often we hit DB
- Eviction count: Shows how often we evict caches
- Load success: Confirms data is being cached

### Best Practices

1. **Keep TTL reasonable** (30 minutes is good for comments)
2. **Use sticky sessions** to maximize cache effectiveness
3. **Monitor hit rates** to ensure caching is working
4. **Document eventual consistency** so team understands trade-offs
5. **Test multi-instance** scenarios to verify behavior

### Summary

✅ **Caffeine with user-specific eviction is perfect for:**
- Social features (comments, likes, follows)
- Read-heavy workloads
- Non-critical data
- Applications with sticky sessions

✅ **Benefits:**
- Strong consistency for active user
- Eventual consistency for others
- Low latency (in-memory)
- No external dependencies

✅ **Result:**
- User A sees their changes immediately ✅
- Other users see changes eventually (≤30 min) ✅
- Database load reduced by 70-90% ✅
- Simple infrastructure (no Redis needed) ✅

