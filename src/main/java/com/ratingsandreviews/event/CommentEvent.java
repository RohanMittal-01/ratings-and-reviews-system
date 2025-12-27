package com.ratingsandreviews.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private EventType eventType;
    private UUID commentId;
    private UUID applicationId;
    private UUID userId;
    private String text;
    private Short sentiment;
    private UUID parentId;
    private Long level;
    private ZonedDateTime timestamp;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}

