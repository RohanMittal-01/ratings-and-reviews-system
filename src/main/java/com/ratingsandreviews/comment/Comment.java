package com.ratingsandreviews.comment;



import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID applicationId;
    private UUID userId;
    private String text;
    private Short sentiment; // 1=positive, -1=negative, null for regular comments
    private long level; // 0 for review/root, >0 for nested
    private UUID parentId; // null for root
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    @Getter
    @Setter
    @Transient
    private List<Comment> children = new ArrayList<>();

    public static final int POSITIVE = 1;
    public static final int NEGATIVE = -1;

    // Optionally, helper methods for type safety
    public void setSentimentPositive() { this.sentiment = POSITIVE; }
    public void setSentimentNegative() { this.sentiment = NEGATIVE; }
    public boolean isPositive() { return Integer.valueOf(POSITIVE).equals(this.sentiment); }
    public boolean isNegative() { return Integer.valueOf(NEGATIVE).equals(sentiment); }
}
