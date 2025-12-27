package com.ratingsandreviews.event;

import com.ratingsandreviews.comment.Comment;
import com.ratingsandreviews.comment.CommentRepository;
import com.ratingsandreviews.util.AppLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CommentEventConsumer {
    private static final AppLogger logger = AppLogger.getInstance(CommentEventConsumer.class);

    private final CommentRepository repository;

    @Autowired
    public CommentEventConsumer(CommentRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "comment-events", groupId = "comment-persistence-group")
    public void consumeCommentEvent(CommentEvent event) {
        try {
            logger.info("Received event: " + event.getEventType() + " for comment: " + event.getCommentId());

            switch (event.getEventType()) {
                case CREATED:
                    handleCreate(event);
                    break;
                case UPDATED:
                    handleUpdate(event);
                    break;
                case DELETED:
                    handleDelete(event);
                    break;
                default:
                    logger.warn("Unknown event type: " + event.getEventType());
            }

            logger.info("Successfully processed event: " + event.getEventType() + " for comment: " + event.getCommentId());
        } catch (Exception e) {
            logger.error("Error processing comment event: " + event.getCommentId(), e);
            // Kafka will retry based on configuration
            throw new RuntimeException("Failed to process comment event", e);
        }
    }

    private void handleCreate(CommentEvent event) {
        Comment comment = new Comment();
        comment.setId(event.getCommentId());
        comment.setApplicationId(event.getApplicationId());
        comment.setUserId(event.getUserId());
        comment.setText(event.getText());
        comment.setSentiment(event.getSentiment());
        comment.setParentId(event.getParentId());
        comment.setLevel(event.getLevel());
        comment.setCreatedAt(event.getTimestamp());
        comment.setUpdatedAt(event.getTimestamp());
        repository.save(comment);
    }

    private void handleUpdate(CommentEvent event) {
        Optional<Comment> existingOpt = repository.findById(event.getCommentId());
        if (existingOpt.isPresent()) {
            Comment existing = existingOpt.get();
            if (event.getText() != null) {
                existing.setText(event.getText());
            }
            if (event.getSentiment() != null && existing.getLevel() == 0) {
                existing.setSentiment(event.getSentiment());
            }
            existing.setUpdatedAt(event.getTimestamp());
            repository.save(existing);
        } else {
            logger.warn("Comment not found for update: " + event.getCommentId());
        }
    }

    private void handleDelete(CommentEvent event) {
        repository.deleteById(event.getCommentId());
    }
}

