package com.ratingsandreviews.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/comments")
public class CommentController {
    private final CommentService service;

    @Autowired
    public CommentController(CommentService service) {
        this.service = service;
    }

    // Get paginated, sorted comments for an application (parentId=null for root/review, else for nested)
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<Page<Comment>> getComments(
            @PathVariable UUID applicationId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) Integer sentiment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) UUID userId) {
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Comment> commentPage = service.getComments(applicationId, parentId, sentiment, pageable, userId);
        return ResponseEntity.ok(commentPage);
    }

    // Get user's comments for a application, with context (ancestor chain)
    @GetMapping("/user/{userId}/application/{applicationId}")
    public ResponseEntity<UserCommentsResponse> getUserCommentsForApplication(
            @PathVariable UUID applicationId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer sentiment) {
        Pageable pageable = PageRequest.of(page, size);
        UserCommentsResponse response = service.getUserCommentsForApplication(applicationId, userId, pageable, sentiment);
        return ResponseEntity.ok(response);
    }

    // Add a comment or review (parentId null for review)
    @PostMapping
    public ResponseEntity<Page<Comment>> addComment(@RequestBody AddCommentRequest commentRequest) {
        // use CommentBuilder to create Comment object from the request
        Comment comment = new CommentBuilder()
                .applicationId(UUID.fromString(commentRequest.applicationId()))
                .parentId(commentRequest.parentId() != null ? UUID.fromString(commentRequest.parentId()) : null)
                .sentiment(commentRequest.sentiment())
                .userId(UUID.fromString(commentRequest.userId()))
                .text(commentRequest.text())
                .build();
        Comment saved = service.addComment(comment);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
        Page<Comment> page = service.getComments(saved.getApplicationId(), comment.getParentId(), null, pageable, null);
        return ResponseEntity.ok(page);
    }

    // Update a comment or review
    @PutMapping("/{id}")
    public ResponseEntity<Page<Comment>> updateComment(@PathVariable UUID id, @RequestBody UpdateCommentRequest updated) {
        Comment saved = service.updateComment(id, updated.text, updated.sentiment);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
        Page<Comment> page = service.getComments(saved.getApplicationId(), saved.getParentId(), null, pageable, null);
        return ResponseEntity.ok(page);
    }

    // Delete a comment or review
    @DeleteMapping("/{id}")
    public ResponseEntity<Page<Comment>> deleteComment(@PathVariable UUID id, @RequestParam UUID applicationId, @RequestParam(required = false) UUID parentId) {
        service.deleteComment(id);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
        Page<Comment> page = service.getComments(applicationId, parentId, null, pageable, null);
        return ResponseEntity.ok(page);
    }

    // Get the full comment tree for an application
    @GetMapping("/tree/{applicationId}")
    public List<Comment> getCommentTree(
            @PathVariable String applicationId,
            @RequestParam(required = false) String userId) {
        // assuming we receive userId from the authentication context in a real scenario
        return service.getCommentTree(UUID.fromString(applicationId), UUID.fromString(userId));
    }

    public record AddCommentRequest(String applicationId, String parentId, Short sentiment, String userId, String text) {
    }

    public record UpdateCommentRequest(String text, Short sentiment) {
    }
}
