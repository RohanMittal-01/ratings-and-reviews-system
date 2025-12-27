package com.ratingsandreviews.comment;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CommentBuilder {
    private UUID id;
    private UUID applicationId;
    private UUID userId;
    private String text;
    private UUID parentId;
    private int level;
    private Short sentiment; // 1 for positive, -1 for negative
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public CommentBuilder id(UUID id) { this.id = id; return this; }
    public CommentBuilder applicationId(UUID applicationId) { this.applicationId = applicationId; return this; }
    public CommentBuilder userId(UUID userId) { this.userId = userId; return this; }
    public CommentBuilder text(String text) { this.text = text; return this; }
    public CommentBuilder parentId(UUID parentId) { this.parentId = parentId; return this; }
    public CommentBuilder level(int level) { this.level = level; return this; }
    public CommentBuilder sentiment(Short sentiment) { this.sentiment = sentiment; return this; }
    public CommentBuilder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
    public CommentBuilder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

    public Comment build() {
        Comment rc = new Comment();
        rc.setId(id);
        rc.setApplicationId(applicationId);
        rc.setUserId(userId);
        rc.setText(text);
        rc.setParentId(parentId);
        rc.setLevel(level);
        rc.setSentiment(sentiment);
        rc.setCreatedAt(createdAt);
        rc.setUpdatedAt(updatedAt);
        return rc;
    }
}
