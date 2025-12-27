package com.ratingsandreviews.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    // Fetch comments for a post, optionally filtered by sentiment, parentId, or userId
    Page<Comment> findByApplicationIdAndParentIdAndSentiment(UUID applicationId, UUID parentId, Integer sentiment, Pageable pageable);
    Page<Comment> findByApplicationIdAndParentId(UUID applicationId, UUID parentId, Pageable pageable);
    Page<Comment> findByApplicationIdAndUserIdAndSentiment(UUID applicationId, UUID userId, Integer sentiment, Pageable pageable);
    Page<Comment> findByApplicationIdAndUserId(UUID applicationId, UUID userId, Pageable pageable);
    Page<Comment> findByApplicationId(UUID applicationId, Pageable pageable);

    // Recursive query to fetch ancestors for a list of comment IDs
    @Query(value = """
        WITH RECURSIVE ancestors AS (
            SELECT c.id, c.parent_id, c.application_id, c.user_id, c.text, c.sentiment, c.level, c.created_at, c.updated_at, 0 as lineage_level
            FROM comments c
            WHERE c.id IN :commentIds
            UNION ALL
            SELECT p.id, p.parent_id, p.application_id, p.user_id, p.text, p.sentiment, p.level, p.created_at, p.updated_at, a.lineage_level - 1
            FROM comments p
            INNER JOIN ancestors a ON p.id = a.parent_id
        )
        SELECT * FROM ancestors ORDER BY id ASC
    """, nativeQuery = true)
    List<Comment> findAncestorsForComments(@Param("commentIds") List<UUID> commentIds);
}
