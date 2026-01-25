package uz.vv.commentservice

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
    val createdAt: java.time.Instant?,
    val hasReplies: Boolean = false,
    val likeCount: Long = 0
)