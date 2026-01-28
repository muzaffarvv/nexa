package uz.vv.commentservice

import java.time.Instant

data class CommentCreateDto(
    val postId: Long,
    val userId: Long,
    val parentId: Long? = null,
    val content: String
)

data class CommentResponseDto(
    val id: Long,
    val userId: Long,
    val postId: Long,
    val parentId: Long?,
    val content: String,
    val createdAt: Instant?,
    val hasReplies: Boolean = false,
    val likeCount: Long = 0
)