package uz.vv.postservice

import org.springframework.stereotype.Component

interface BaseMapper<E, R> {
    fun toDto(entity: E): R
    fun toDtoList(entities: List<E>): List<R> = entities.map { toDto(it) }
}

@Component
class PostMapper : BaseMapper<Post, PostResponseDto> {

    override fun toDto(entity: Post): PostResponseDto {
        return PostResponseDto(
            id = entity.id!!,
            userId = entity.userId,
            username = "Unknown", // Service layer will enrich this
            parentId = entity.parentId,
            content = entity.content,
            mediaUrl = entity.mediaUrl,
            createdAt = entity.createdAt,
            hasSubPosts = false, // Service layer will enrich this
            commentCount = 0, // Service layer will enrich this
            likeCount = 0, // Service layer will enrich this
            archived = entity.archived
        )
    }
}