package com.ratingsandreviews.event;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
public class CommentEventDeserializer implements Deserializer<CommentEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    @Override
    public CommentEvent deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.readValue(data, CommentEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing CommentEvent", e);
        }
    }
}
