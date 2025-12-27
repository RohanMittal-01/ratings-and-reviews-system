package com.ratingsandreviews.comment;

import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface CommentService {
    Page<Comment> getComments(UUID postId, UUID parentId, Integer sentiment, Pageable pageable, UUID userId);
    Comment addComment(Comment comment);
    Comment updateComment(UUID id, String updatedText, Short sentiment);
    void deleteComment(UUID id);
    List<Comment> getCommentTree(UUID postId, UUID userId);
    UserCommentsResponse getUserCommentsForApplication(UUID postId, UUID userId, Pageable pageable, Integer sentiment);
}

@Getter
class CommentThreadView {
    private final Comment target;
    private final List<Comment> context;
    public CommentThreadView(Comment target, List<Comment> context) {
        this.target = target;
        this.context = context;
    }
}