package com.ratingsandreviews.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.ratingsandreviews.util.Validations.validateOptionalExistence;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository repository;

    @Autowired
    public CommentServiceImpl(CommentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Comment> getComments(UUID applicationId, UUID parentId, Integer sentiment, Pageable pageable, UUID userId) {
        if (userId != null) {
            // 1. Get user's comments and their context (full chain from root to user comment)
            UserCommentsResponse userResp = getUserCommentsForApplication(applicationId, userId, pageable, sentiment);
            Set<UUID> userAndContextIds = new HashSet<>();
            Map<UUID, Comment> rootMap = new LinkedHashMap<>(); // rootId -> root Comment (with children)
            for (CommentThreadView thread : userResp.getThreads()) {
                List<Comment> chain = new ArrayList<>(thread.getContext());
                chain.add(thread.getTarget());
                if (chain.isEmpty()) continue;
                Comment root = chain.get(0);
                // Traverse down the chain, building/attaching children
                Comment current = rootMap.computeIfAbsent(root.getId(), id -> cloneCommentWithoutChildren(root));
                for (int i = 1; i < chain.size(); i++) {
                    Comment next = chain.get(i);
                    // Find or add child to current
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
            // 2. Fill the rest with other root comments (level 0, not already included)
            List<Comment> others = repository.findByApplicationId(applicationId, pageable).stream()
                .filter(c -> c.getLevel() == 0 && !userAndContextIds.contains(c.getId()))
                .toList();
            List<Comment> result = new ArrayList<>(rootMap.values());
            result.addAll(others);
            // Paginate result manually
            int from = (int) pageable.getOffset();
            int to = Math.min(from + pageable.getPageSize(), result.size());
            List<Comment> pageContent = from < to ? result.subList(from, to) : Collections.emptyList();
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, result.size());
        } else {
            // Default logic
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
        comment.setCreatedAt(Instant.now().atZone(java.time.ZoneOffset.UTC));
        comment.setUpdatedAt(Instant.now().atZone(java.time.ZoneOffset.UTC));
        if (comment.getParentId() == null) {
            comment.setLevel(0);
        } else {
            Comment parent = validateOptionalExistence(repository.findById(comment.getParentId()), Comment.class, "Parent commment");
            long parentLevel = parent.getLevel();
            comment.setLevel(parentLevel + 1);
        }
        return repository.save(comment);
    }

    @Override
    public Comment updateComment(UUID id, String updatedText, Short sentiment) {
        Comment existing = validateOptionalExistence(repository.findById(id), Comment.class, "Comment");
        if(updatedText != null) existing.setText(updatedText);
        if(sentiment != null && existing.getLevel() == 0) existing.setSentiment(sentiment);
        return repository.save(existing);
    }

    @Override
    public void deleteComment(UUID id) {
        // handled cascading delete via column definition
        repository.deleteById(id);
    }

    @Override
    public List<Comment> getCommentTree(UUID applicationId, UUID userId) {
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
        return new UserCommentsResponse(threads, userCommentsPage.getTotalElements());
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
}
