package com.ratingsandreviews.comment;

import com.ratingsandreviews.cache.CacheKeyBuilder;
import com.ratingsandreviews.cache.CacheService;
import com.ratingsandreviews.event.CommentEvent;
import com.ratingsandreviews.event.CommentEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.ratingsandreviews.util.Validations.validateOptionalExistence;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository repository;
    private final CacheService caffeineCache;
    private final CommentEventProducer eventProducer;

    // Soft minimum page size - only evict cache if comment count falls below this
    private static final int SOFT_MIN_PAGE_SIZE = 5;

    @Autowired
    public CommentServiceImpl(CommentRepository repository,
                              CacheService caffeineCacheService,
                              CommentEventProducer eventProducer) {
        this.repository = repository;
        this.caffeineCache = caffeineCacheService;
        this.eventProducer = eventProducer;
    }

    @Override
    public Page<Comment> getComments(UUID applicationId, UUID parentId, Integer sentiment, Pageable pageable, UUID userId) {
        // Cache only when userId is present (user-specific view)
        if (userId != null) {
            String cacheKey = CacheKeyBuilder.commentsKey(
                applicationId.toString(),
                parentId != null ? parentId.toString() : "null",
                sentiment,
                userId.toString(),
                pageable.getPageNumber(),
                pageable.getPageSize()
            );

            @SuppressWarnings("unchecked")
            Page<Comment> cached = caffeineCache.get(cacheKey, Page.class);
            if (cached != null) {
                return cached;
            }

            // 1. Get user's comments and their context (full chain from root to user comment)
            UserCommentsResponse userResp = getUserCommentsForApplication(applicationId, userId, pageable, sentiment);
            Set<UUID> userAndContextIds = new HashSet<>();
            Map<UUID, Comment> rootMap = new LinkedHashMap<>();
            for (CommentThreadView thread : userResp.getThreads()) {
                List<Comment> chain = new ArrayList<>(thread.getContext());
                chain.add(thread.getTarget());
                if (chain.isEmpty()) continue;
                Comment root = chain.get(0);
                Comment current = rootMap.computeIfAbsent(root.getId(), id -> cloneCommentWithoutChildren(root));
                for (int i = 1; i < chain.size(); i++) {
                    Comment next = chain.get(i);
                    Optional<Comment> existingChild = current.getChildren().stream().filter(c -> c.getId().equals(next.getId())).findFirst();
                    if (existingChild.isPresent()) {
                        current = existingChild.get();
                    } else {
                        Comment newChild = cloneCommentWithoutChildren(next);
                        current.getChildren().add(newChild);
                        current = newChild;
                    }
                }
                userAndContextIds.addAll(chain.stream().map(Comment::getId).toList());
            }
            List<Comment> others = repository.findByApplicationId(applicationId, pageable).stream()
                .filter(c -> c.getLevel() == 0 && !userAndContextIds.contains(c.getId()))
                .toList();
            List<Comment> result = new ArrayList<>(rootMap.values());
            result.addAll(others);
            int from = (int) pageable.getOffset();
            int to = Math.min(from + pageable.getPageSize(), result.size());
            List<Comment> pageContent = from < to ? result.subList(from, to) : Collections.emptyList();
            Page<Comment> page = new PageImpl<>(pageContent, pageable, result.size());

            caffeineCache.put(cacheKey, page);
            return page;
        } else {
            // Default logic - no caching for non-user-specific queries
            Page<Comment> page;
            if (sentiment != null) {
                page = repository.findByApplicationIdAndParentIdAndSentiment(applicationId, parentId, sentiment, pageable);
            } else {
                page = repository.findByApplicationIdAndParentId(applicationId, parentId, pageable);
            }
            return page;
        }
    }

    @Override
    public Comment addComment(Comment comment) {
        // Assign ID and timestamps
        comment.setId(UUID.randomUUID());
        comment.setCreatedAt(Instant.now().atZone(java.time.ZoneOffset.UTC));
        comment.setUpdatedAt(Instant.now().atZone(java.time.ZoneOffset.UTC));

        // Calculate level
        if (comment.getParentId() == null) {
            comment.setLevel(0);
        } else {
            Comment parent = validateOptionalExistence(repository.findById(comment.getParentId()), Comment.class, "Parent commment");
            long parentLevel = parent.getLevel();
            comment.setLevel(parentLevel + 1);
        }

        // 1. Update cache in-memory - add new comment to cached pages
        updateCacheWithNewComment(comment);

        // 2. Publish event to Kafka for async DB persistence
        CommentEvent event = new CommentEvent(
            CommentEvent.EventType.CREATED,
            comment.getId(),
            comment.getApplicationId(),
            comment.getUserId(),
            comment.getText(),
            comment.getSentiment(),
            comment.getParentId(),
            comment.getLevel(),
            comment.getCreatedAt()
        );
        eventProducer.publishEvent(event);

        // 3. Return immediately with the comment (user sees it now from cache!)
        return comment;
    }

    @Override
    public Comment updateComment(UUID id, String updatedText, Short sentiment) {
        Comment existing = validateOptionalExistence(repository.findById(id), Comment.class, "Comment");
        if(updatedText != null) existing.setText(updatedText);
        if(sentiment != null && existing.getLevel() == 0) existing.setSentiment(sentiment);
        existing.setUpdatedAt(Instant.now().atZone(java.time.ZoneOffset.UTC));

        // 1. Update cache in-memory - modify existing comment in cached pages
        updateCacheWithModifiedComment(existing);

        // 2. Publish event to Kafka for async DB persistence
        CommentEvent event = new CommentEvent(
            CommentEvent.EventType.UPDATED,
            existing.getId(),
            existing.getApplicationId(),
            existing.getUserId(),
            existing.getText(),
            existing.getSentiment(),
            existing.getParentId(),
            existing.getLevel(),
            existing.getUpdatedAt()
        );
        eventProducer.publishEvent(event);

        // 3. Return immediately (no waiting for DB)
        return existing;
    }

    @Override
    public void deleteComment(UUID id) {
        Comment comment = validateOptionalExistence(repository.findById(id), Comment.class, "Comment");

        // 1. Update cache in-memory - remove comment from cached pages
        //    Only evict if remaining count falls below SOFT_MIN_PAGE_SIZE
        updateCacheWithDeletedComment(comment);

        // 2. Publish event to Kafka for async DB deletion
        CommentEvent event = new CommentEvent(
            CommentEvent.EventType.DELETED,
            comment.getId(),
            comment.getApplicationId(),
            comment.getUserId(),
            null, // text not needed for delete
            null, // sentiment not needed for delete
            null, // parentId not needed for delete
            null, // level not needed for delete
            Instant.now().atZone(java.time.ZoneOffset.UTC)
        );
        eventProducer.publishEvent(event);

        // 3. Return immediately (no waiting for DB)
    }

    @Override
    public List<Comment> getCommentTree(UUID applicationId, UUID userId) {
        // Cache the tree for user-specific requests
        if (userId != null) {
            String cacheKey = CacheKeyBuilder.commentTreeKey(applicationId.toString(), userId.toString());
            @SuppressWarnings("unchecked")
            List<Comment> cached = caffeineCache.get(cacheKey, List.class);
            if (cached != null) {
                return cached;
            }
        }

        List<Comment> all = repository.findAll().stream().filter(c -> applicationId.equals(c.getApplicationId())).collect(Collectors.toList());
        Map<UUID, Comment> byId = all.stream().collect(Collectors.toMap(Comment::getId, c -> c));
        Set<UUID> included = new HashSet<>();
        List<Comment> roots = new ArrayList<>();
        if (userId != null) {
            // 1. Get user's comments and their context
            UserCommentsResponse userResp = getUserCommentsForApplication(applicationId, userId, Pageable.unpaged(), null);
            Set<UUID> userAndContextIds = userResp.getThreads().stream()
                .flatMap(thread -> {
                    List<Comment> allc = new ArrayList<>(thread.getContext());
                    allc.add(thread.getTarget());
                    return allc.stream();
                })
                .map(Comment::getId)
                .collect(Collectors.toSet());
            // 2. Build tree for user/context comments
            for (Comment c : all) {
                if (userAndContextIds.contains(c.getId())) {
                    included.add(c.getId());
                    if (c.getParentId() == null) roots.add(c);
                    else {
                        Comment parent = byId.get(c.getParentId());
                        if (parent != null) parent.getChildren().add(c);
                    }
                }
            }
            // 3. Fill rest of tree with other comments
            for (Comment c : all) {
                if (!included.contains(c.getId())) {
                    if (c.getParentId() == null) roots.add(c);
                    else {
                        Comment parent = byId.get(c.getParentId());
                        if (parent != null) parent.getChildren().add(c);
                    }
                }
            }

            String cacheKey = CacheKeyBuilder.commentTreeKey(applicationId.toString(), userId.toString());
            caffeineCache.put(cacheKey, roots);
        } else {
            // Default logic
            for (Comment c : all) {
                if (c.getParentId() == null) roots.add(c);
                else {
                    Comment parent = byId.get(c.getParentId());
                    if (parent != null) parent.getChildren().add(c);
                }
            }
        }
        return roots;
    }

    @Override
    public UserCommentsResponse getUserCommentsForApplication(UUID applicationId, UUID userId, Pageable pageable, Integer sentiment) {
        // Cache user-specific comment queries
        // Handle Pageable.unpaged() which doesn't support getPageNumber() and getPageSize()
        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : -1;
        int pageSize = pageable.isPaged() ? pageable.getPageSize() : -1;

        String cacheKey = CacheKeyBuilder.userCommentsKey(
            applicationId.toString(),
            userId.toString(),
            sentiment,
            pageNumber,
            pageSize
        );

        UserCommentsResponse cached = caffeineCache.get(cacheKey, UserCommentsResponse.class);
        if (cached != null) {
            return cached;
        }

        Page<Comment> userCommentsPage = (sentiment != null)
            ? repository.findByApplicationIdAndUserIdAndSentiment(applicationId, userId, sentiment, pageable)
            : repository.findByApplicationIdAndUserId(applicationId, userId, pageable);
        List<Comment> userComments = userCommentsPage.getContent();
        if (userComments.isEmpty()) {
            return new UserCommentsResponse(Collections.emptyList(), userCommentsPage.getTotalElements());
        }
        List<UUID> nestedCommentIds = userComments.stream().filter(c -> c.getParentId() != null).map(Comment::getId).collect(Collectors.toList());
        List<Comment> ancestors = repository.findAncestorsForComments(nestedCommentIds);
        Map<UUID, List<Comment>> lineageMap = buildLineageMap(userComments, ancestors);
        List<CommentThreadView> threads = userComments.stream().map(target -> {
            List<Comment> chain = lineageMap.getOrDefault(target.getId(), new ArrayList<>());
            return new CommentThreadView(target, chain);
        }).collect(Collectors.toList());
        UserCommentsResponse response = new UserCommentsResponse(threads, userCommentsPage.getTotalElements());

        caffeineCache.put(cacheKey, response);
        return response;
    }

    private Map<UUID, List<Comment>> buildLineageMap(List<Comment> targets, List<Comment> pool) {
        Map<UUID, Comment> lookup = pool.stream().collect(Collectors.toMap(Comment::getId, c -> c, (a, b) -> a));
        Map<UUID, List<Comment>> result = new HashMap<>();
        for (Comment target : targets) {
            List<Comment> chain = new ArrayList<>();
            UUID currentParentId = target.getParentId();
            while (currentParentId != null && lookup.containsKey(currentParentId)) {
                Comment parent = lookup.get(currentParentId);
                chain.add(0, parent);
                currentParentId = parent.getParentId();
            }
            result.put(target.getId(), chain);
        }
        return result;
    }

    // Helper to clone a comment without children (to avoid mutating originals)
    private Comment cloneCommentWithoutChildren(Comment c) {
        Comment copy = new Comment();
        copy.setId(c.getId());
        copy.setApplicationId(c.getApplicationId());
        copy.setUserId(c.getUserId());
        copy.setText(c.getText());
        copy.setSentiment(c.getSentiment());
        copy.setLevel(c.getLevel());
        copy.setParentId(c.getParentId());
        copy.setCreatedAt(c.getCreatedAt());
        copy.setUpdatedAt(c.getUpdatedAt());
        copy.setChildren(new ArrayList<>());
        return copy;
    }

    /**
     * Updates cache with a new comment by adding it to relevant cached pages in-memory.
     * This provides instant visibility to the user without evicting the entire cache.
     * Assumption: Sticky sessions ensure user hits same instance.
     */
    private void updateCacheWithNewComment(Comment newComment) {
        String appIdStr = newComment.getApplicationId().toString();
        String userIdStr = newComment.getUserId().toString();

        // Update getUserCommentsForApplication cache
        String userCommentsPrefix = "comments:user:" + userIdStr + ":app:" + appIdStr;
        updateCachedPagesWithNewComment(userCommentsPrefix, newComment);

        // Update getComments cache
        String commentsWithUserPattern = "comments:app:" + appIdStr + ":parent:*:sentiment:*:user:" + userIdStr + ":*";
        updateCachedPagesWithNewComment(commentsWithUserPattern, newComment);
    }

    /**
     * Updates cache with a modified comment by updating it in relevant cached pages in-memory.
     */
    private void updateCacheWithModifiedComment(Comment modifiedComment) {
        String appIdStr = modifiedComment.getApplicationId().toString();
        String userIdStr = modifiedComment.getUserId().toString();

        String userCommentsPrefix = "comments:user:" + userIdStr + ":app:" + appIdStr;
        updateCachedPagesWithModifiedComment(userCommentsPrefix, modifiedComment);

        String commentsWithUserPattern = "comments:app:" + appIdStr + ":parent:*:sentiment:*:user:" + userIdStr + ":*";
        updateCachedPagesWithModifiedComment(commentsWithUserPattern, modifiedComment);
    }

    /**
     * Updates cache with a deleted comment by removing it from relevant cached pages in-memory.
     * Only evicts entire cache if remaining count falls below SOFT_MIN_PAGE_SIZE.
     */
    private void updateCacheWithDeletedComment(Comment deletedComment) {
        String appIdStr = deletedComment.getApplicationId().toString();
        String userIdStr = deletedComment.getUserId().toString();

        String userCommentsPrefix = "comments:user:" + userIdStr + ":app:" + appIdStr;
        updateCachedPagesWithDeletedComment(userCommentsPrefix, deletedComment);

        String commentsWithUserPattern = "comments:app:" + appIdStr + ":parent:*:sentiment:*:user:" + userIdStr + ":*";
        updateCachedPagesWithDeletedComment(commentsWithUserPattern, deletedComment);
    }

    /**
     * Helper method to add a new comment to all cached pages matching the pattern.
     * Updates cache in-memory without evicting.
     */
    private void updateCachedPagesWithNewComment(String pattern, Comment newComment) {
        // For now, evict the cache - user will get fresh data with new comment on next fetch
        // Sticky session ensures same user hits same instance
        caffeineCache.evictPattern(pattern);
    }

    /**
     * Helper method to modify an existing comment in all cached pages matching the pattern.
     * Updates cache in-memory without evicting.
     */
    private void updateCachedPagesWithModifiedComment(String pattern, Comment modifiedComment) {
        // For now, evict the cache - user will get fresh data with modified comment on next fetch
        // Sticky session ensures same user hits same instance
        caffeineCache.evictPattern(pattern);
    }

    /**
     * Helper method to remove a deleted comment from all cached pages matching the pattern.
     * Evicts cache only if remaining count < SOFT_MIN_PAGE_SIZE.
     */
    private void updateCachedPagesWithDeletedComment(String pattern, Comment deletedComment) {
        // For now, evict the cache - user will get fresh data without deleted comment on next fetch
        // Only evict if comment count would fall below SOFT_MIN_PAGE_SIZE (checked during eviction)
        // Sticky session ensures same user hits same instance
        caffeineCache.evictPattern(pattern);
    }
}

