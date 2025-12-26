package com.ratingsandreviews.application;

import java.util.HashMap;
import java.util.Map;

public class FilterStrategyFactory {
    private static final Map<String, FilterStrategy> strategies = new HashMap<>();
    static {
        strategies.put("name", new NameFilterStrategy());
        strategies.put("all", new AllFilterStrategy());
        // Add more strategies as needed
    }

    public static FilterStrategy getStrategy(String filterKey) {
        return strategies.getOrDefault(filterKey, new AllFilterStrategy());
    }
}
