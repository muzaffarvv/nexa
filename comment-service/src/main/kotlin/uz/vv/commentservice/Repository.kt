package uz.vv.commentservice

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CommentRepo : JpaRepository<Comment, Long> {

    @Query("""
        SELECT c FROM Comment c 
        WHERE c.postId = :postId 
        AND c.deleted = false 
        AND c.parentId IS NULL
        ORDER BY c.createdAt DESC
    """)
    fun findTopLevelCommentsByPost(
        @Param("postId") postId: Long,
        pageable: Pageable
    ): Page<Comment>

    // ✅ Replies uchun optimizatsiya qilingan
    @Query("""
        SELECT c FROM Comment c
        WHERE c.parentId = :parentId
        AND c.deleted = false
        ORDER BY c.createdAt ASC
    """)
    fun findRepliesByParent(@Param("parentId") parentId: Long): List<Comment>

    // ✅ Bitta comment olish (deleted check bilan)
    @Query("""
        SELECT c FROM Comment c
        WHERE c.id = :id
        AND c.deleted = false
    """)
    fun findByIdAndNotDeleted(@Param("id") id: Long): Comment?

    // ✅ Parent comment mavjudligini tekshirish
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Comment c
        WHERE c.id = :id
        AND c.deleted = false
    """)
    fun existsByIdAndNotDeleted(@Param("id") id: Long): Boolean

    // ✅ Bulk soft delete (cascade uchun)
    @Modifying
    @Query("""
        UPDATE Comment c 
        SET c.deleted = true 
        WHERE c.parentId = :parentId
        AND c.deleted = false
    """)
    fun softDeleteReplies(@Param("parentId") parentId: Long): Int

    @Query("""
        SELECT COUNT(c) FROM Comment c
        WHERE c.postId = :postId
        AND c.deleted = false
    """)
    fun countByPostId(@Param("postId") postId: Long): Long
}

@Repository
interface CommentStatsRepo : JpaRepository<CommentStats, Long> {

    @Modifying
    @Query("""
        update CommentStats cs
        set cs.mediaKey = :mediaKey
        where cs.commentId = :commentId
          and cs.deleted = false
          and cs.status = 'ACTIVE'
    """)
    fun updateMediaKeyByCommentId(
        @Param("commentId") commentId: Long,
        @Param("mediaKey") mediaKey: String
    ): Int

    // ✅ Bitta stats olish
    fun findByCommentId(commentId: Long): CommentStats?

    // ✅ Batch fetch - N+1 muammosini hal qiladi
    @Query("""
        SELECT cs FROM CommentStats cs 
        WHERE cs.commentId IN :commentIds
    """)
    fun findByCommentIdIn(@Param("commentIds") commentIds: List<Long>): List<CommentStats>

    // ✅ hasReplies ni true qilish
    @Modifying
    @Query("""
        UPDATE CommentStats s 
        SET s.hasReplies = true 
        WHERE s.commentId = :commentId
    """)
    fun markHasReplies(@Param("commentId") commentId: Long)

    // ✅ Like count increment
    @Modifying
    @Query("""
        UPDATE CommentStats s 
        SET s.likeCount = s.likeCount + 1 
        WHERE s.commentId = :commentId
    """)
    fun incrementLikeCount(@Param("commentId") commentId: Long)

    // ✅ Like count decrement
    @Modifying
    @Query("""
        UPDATE CommentStats s 
        SET s.likeCount = s.likeCount - 1 
        WHERE s.commentId = :commentId
        AND s.likeCount > 0
    """)
    fun decrementLikeCount(@Param("commentId") commentId: Long)

    // ✅ Bulk stats yaratish
    @Modifying
    @Query(value = """
        INSERT INTO comment_stats (comment_id, user_id, username, post_id, content, has_replies, like_count, created_at, status, deleted)
        VALUES (:commentId, :userId, :username, :postId, :content, false, 0, NOW(), 'ACTIVE', false)
    """, nativeQuery = true)
    fun createStats(
        @Param("commentId") commentId: Long,
        @Param("userId") userId: Long,
        @Param("username") username: String,
        @Param("postId") postId: Long,
        @Param("content") content: String
    )
}