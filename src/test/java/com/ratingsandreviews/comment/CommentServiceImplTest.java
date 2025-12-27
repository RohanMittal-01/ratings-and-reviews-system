package com.ratingsandreviews.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentServiceImplTest {
    @Mock
    private CommentRepository repository;

    @InjectMocks
    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addComment_setsLevelAndSaves() {
        Comment parent = new Comment();
        parent.setId(UUID.randomUUID());
        parent.setLevel(0);
        Comment child = new Comment();
        child.setParentId(parent.getId());
        when(repository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(repository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        Comment saved = service.addComment(child);
        assertEquals(1, saved.getLevel());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void addComment_root_setsLevelZero() {
        Comment root = new Comment();
        root.setParentId(null);
        when(repository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        Comment saved = service.addComment(root);
        assertEquals(0, saved.getLevel());
    }

    @Test
    void updateComment_updatesTextAndSentiment() {
        Comment existing = new Comment();
        existing.setId(UUID.randomUUID());
        existing.setLevel(0);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        Comment updated = service.updateComment(existing.getId(), "new text", (short) 1);
        assertEquals("new text", updated.getText());
        assertEquals(Short.valueOf((short)1), updated.getSentiment());
    }

    @Test
    void deleteComment_deletesById() {
        UUID id = UUID.randomUUID();
        service.deleteComment(id);
        verify(repository).deleteById(id);
    }

    @Test
    void getComments_userFirst_groupsByRootAndChildren() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment root = new Comment();
        root.setId(UUID.randomUUID());
        root.setApplicationId(appId);
        root.setLevel(0);
        Comment userComment = new Comment();
        userComment.setId(UUID.randomUUID());
        userComment.setApplicationId(appId);
        userComment.setParentId(root.getId());
        userComment.setLevel(1);
        userComment.setUserId(userId);
        Page<Comment> userPage = new PageImpl<>(List.of(userComment));
        when(repository.findByApplicationIdAndUserId(appId, userId, PageRequest.of(0, 10))).thenReturn(userPage);
        when(repository.findAncestorsForComments(anyList())).thenReturn(List.of(root));
        when(repository.findByApplicationId(appId, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(root)));
        Page<Comment> result = service.getComments(appId, null, null, PageRequest.of(0, 10), userId);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChildren()).isNotEmpty();
        assertThat(result.getContent().get(0).getChildren().get(0).getId()).isEqualTo(userComment.getId());
    }

    @Test
    void getComments_defaultLogic_returnsPage() {
        UUID appId = UUID.randomUUID();
        Comment root = new Comment();
        root.setId(UUID.randomUUID());
        root.setApplicationId(appId);
        root.setLevel(0);
        Page<Comment> page = new PageImpl<>(List.of(root));
        when(repository.findByApplicationIdAndParentId(appId, null, PageRequest.of(0, 10))).thenReturn(page);
        Page<Comment> result = service.getComments(appId, null, null, PageRequest.of(0, 10), null);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getCommentTree_userFirst_groupsByRootAndChildren() {
        UUID appId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Comment root = new Comment();
        root.setId(UUID.randomUUID());
        root.setApplicationId(appId);
        root.setLevel(0);
        Comment userComment = new Comment();
        userComment.setId(UUID.randomUUID());
        userComment.setApplicationId(appId);
        userComment.setParentId(root.getId());
        userComment.setLevel(1);
        userComment.setUserId(userId);
        when(repository.findByApplicationIdAndUserId(appId, userId, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(userComment)));
        when(repository.findAncestorsForComments(anyList())).thenReturn(List.of(root));
        when(repository.findAll()).thenReturn(List.of(root, userComment));
        List<Comment> result = service.getCommentTree(appId, userId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).isNotEmpty();
        assertThat(result.get(0).getChildren().get(0).getId()).isEqualTo(userComment.getId());
    }

    @Test
    void getCommentTree_defaultLogic_returnsRoots() {
        UUID appId = UUID.randomUUID();
        Comment root = new Comment();
        root.setId(UUID.randomUUID());
        root.setApplicationId(appId);
        root.setLevel(0);
        when(repository.findAll()).thenReturn(List.of(root));
        List<Comment> result = service.getCommentTree(appId, null);
        assertThat(result).hasSize(1);
    }
}
