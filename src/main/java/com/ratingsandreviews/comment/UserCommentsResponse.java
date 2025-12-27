package com.ratingsandreviews.comment;

import lombok.Getter;

import java.util.List;

@Getter
public class UserCommentsResponse {
    private final List<CommentThreadView> threads;
    private final long totalElements;
    public UserCommentsResponse(List<CommentThreadView> threads, long totalElements) {
        this.threads = threads;
        this.totalElements = totalElements;
    }
}
