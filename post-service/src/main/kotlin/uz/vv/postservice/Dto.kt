package uz.vv.postservice


import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class PostCreateDto(
    val userId: Long, // Post egasi

    val parentId: Long? = null, // Agar repost yoki thread bo'lsa

    @field:NotBlank(message = "Content bo'sh bo'lishi mumkin emas")
    @field:Size(max = 5000, message = "Post mazmuni juda uzun")
    val content: String,

    val mediaUrl: String? = null // Media-service'dan olingan URL
)

data class PostUpdateDto(
    @field:NotBlank
    val content: String,
    val mediaUrl: String? = null
)
data class PostResponseDto(
    val id: Long,
    val userId: Long,
    val username: String,
    val parentId: Long?,
    val content: String,
    val mediaUrl: String?,
    val createdAt: Instant?,

    val hasSubPosts: Boolean = false,
    val commentCount: Long = 0,
    val likeCount: Long = 0,

    val archived: Boolean = false
)

data class LikeResponseDto(
    val liked: Boolean,
    val totalLikes: Long
)

data class PostStatsResponseDto(
    val postId: Long,
    val likeCount: Long,
    val commentCount: Long,
    val replyCount: Long,
    val viewCount: Long = 0
)
