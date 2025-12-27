package com.ratingsandreviews.event;

import com.ratingsandreviews.util.AppLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CommentEventProducer {
    private static final AppLogger logger = AppLogger.getInstance(CommentEventProducer.class);
    private static final String TOPIC = "comment-events";

    private final KafkaTemplate<String, CommentEvent> kafkaTemplate;

    @Autowired
    public CommentEventProducer(KafkaTemplate<String, CommentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(CommentEvent event) {
        try {
            String key = event.getCommentId().toString();
            kafkaTemplate.send(TOPIC, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published event: " + event.getEventType() + " for comment: " + event.getCommentId());
                        } else {
                            logger.error("Failed to publish event: " + event.getEventType() + " for comment: " + event.getCommentId(), ex);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error publishing comment event", e);
            throw new RuntimeException("Failed to publish comment event", e);
        }
    }
}
