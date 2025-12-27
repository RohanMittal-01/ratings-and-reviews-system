package com.ratingsandreviews.util;

import java.util.Optional;

public class Validations {
    private Validations() {
        // Private constructor to prevent instantiation
    }
    public static <T> T validateOptionalExistence(Optional<T> optional, Class<T> clazz, String identifier) {
        if (optional.isEmpty()) {
            throw new RuntimeException(
                    String.format("%s with identifier %s not found", clazz.getSimpleName(), identifier)
            );
        }
        return optional.get();
    }
}