package com.ratingsandreviews.cache;

public class CacheKeyBuilder {
    private static final String DELIMITER = ":";

    // Comment cache keys
    public static String userCommentsKey(String applicationId, String userId, Integer sentiment, int page, int size) {
        return "comments:user" + DELIMITER + userId + DELIMITER + "app" + DELIMITER + applicationId +
                DELIMITER + "sentiment" + DELIMITER + sentiment + DELIMITER + "page" + DELIMITER + page + DELIMITER + size;
    }

    public static String userCommentsPattern(String userId) {
        return "comments:user" + DELIMITER + userId;
    }

    public static String commentsKey(String applicationId, String parentId, Integer sentiment, String userId, int page, int size) {
        return "comments:app" + DELIMITER + applicationId + DELIMITER + "parent" + DELIMITER + parentId +
                DELIMITER + "sentiment" + DELIMITER + sentiment + DELIMITER + "user" + DELIMITER + userId +
                DELIMITER + "page" + DELIMITER + page + DELIMITER + size;
    }

    public static String commentsPatternForUser(String applicationId, String userId) {
        return "comments:app" + DELIMITER + applicationId + DELIMITER + "parent" + DELIMITER + "*" +
                DELIMITER + "sentiment" + DELIMITER + "*" + DELIMITER + "user" + DELIMITER + userId;
    }

    public static String commentTreeKey(String applicationId, String userId) {
        return "comments:tree" + DELIMITER + applicationId + DELIMITER + "user" + DELIMITER + userId;
    }

    public static String commentTreePattern(String applicationId) {
        return "comments:tree" + DELIMITER + applicationId;
    }

    // Rating cache keys
    public static String ratingAvgKey(String applicationId) {
        return "rating:avg" + DELIMITER + applicationId;
    }

    public static String ratingStatsKey(String applicationId) {
        return "rating:stats" + DELIMITER + applicationId;
    }

    public static String ratingsPageKey(String applicationId, int page, int size) {
        return "rating:page" + DELIMITER + applicationId + DELIMITER + page + DELIMITER + size;
    }

    public static String ratingsPattern(String applicationId) {
        return "rating:*" + DELIMITER + applicationId;
    }

    // Application cache keys
    public static String applicationKey(String applicationId) {
        return "application" + DELIMITER + applicationId;
    }

    public static String applicationsPageKey(String filterKey, String filterValue, int page, int size) {
        return "applications" + DELIMITER + filterKey + DELIMITER + filterValue + DELIMITER + page + DELIMITER + size;
    }
}
