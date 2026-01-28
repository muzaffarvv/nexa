package uz.vv.commentservice

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepo: CommentRepo,
    private val statsRepo: CommentStatsRepo,
    private val userFeignClient: UserFeignClient,
    private val reactionFeignClient: ReactionFeignClient
) {

    @Transactional
    fun create(dto: CommentCreateDto, currentUserId: Long): CommentResponseDto {
        val user = fetchUserInfo(currentUserId)

        validateParentComment(dto.parentId, dto.postId)

        val comment = commentRepo.save(
            Comment(
                userId = currentUserId,
                postId = dto.postId,
                parentId = dto.parentId,
                content = dto.content.trim()
            )
        )

        statsRepo.createStats(
            commentId = comment.id!!,
            userId = currentUserId,
            username = user.username,
            postId = dto.postId,
            content = dto.content.trim()
        )

        if (dto.parentId != null) {
            statsRepo.markHasReplies(dto.parentId)
        }

        val stats = statsRepo.findByCommentId(comment.id!!) ?: createDefaultStats(comment)
        return mapToDto(comment, stats)
    }

    @Transactional(readOnly = true)
    fun getPostComments(postId: Long, pageable: Pageable): Page<CommentResponseDto> {
        val commentsPage = commentRepo.findTopLevelCommentsByPost(postId, pageable)
        val commentIds = commentsPage.content.mapNotNull { it.id }

        if (commentIds.isEmpty()) return commentsPage.map { mapToDto(it, createDefaultStats(it)) }

        val statsMap = statsRepo.findByCommentIdIn(commentIds).associateBy { it.commentId }

        return commentsPage.map { comment ->
            mapToDto(comment, statsMap[comment.id!!] ?: createDefaultStats(comment))
        }
    }

    @Transactional(readOnly = true)
    fun getReplies(parentId: Long): List<CommentResponseDto> {
        if (!commentRepo.existsByIdAndNotDeleted(parentId)) {
            throw CommentNotFoundException(ErrorCodes.PARENT_COMMENT_NOT_FOUND)
        }

        val replies = commentRepo.findRepliesByParent(parentId)
        val commentIds = replies.mapNotNull { it.id }

        if (commentIds.isEmpty()) return emptyList()

        val statsMap = statsRepo.findByCommentIdIn(commentIds).associateBy { it.commentId }

        return replies.map { reply ->
            mapToDto(reply, statsMap[reply.id!!] ?: createDefaultStats(reply))
        }
    }

    @Transactional
    fun toggleLike(commentId: Long, currentUserId: Long): Boolean {
        if (!commentRepo.existsByIdAndNotDeleted(commentId)) {
            throw CommentNotFoundException()
        }

        val liked = reactionFeignClient.toggleLike(
            userId = currentUserId,
            targetType = ReactionTargetType.COMMENT,
            targetId = commentId
        )

        if (liked) {
            statsRepo.incrementLikeCount(commentId)
        } else {
            statsRepo.decrementLikeCount(commentId)
        }
        return liked
    }

    @Transactional
    fun delete(commentId: Long, currentUserId: Long) {
        val comment = commentRepo.findByIdAndNotDeleted(commentId)
            ?: throw CommentNotFoundException()

        if (comment.userId != currentUserId) {
            throw CommentAccessDeniedException()
        }

        comment.deleted = true
        commentRepo.save(comment)

        commentRepo.softDeleteReplies(commentId)
    }

    private fun fetchUserInfo(userId: Long) = userFeignClient.getUserById(userId)

    private fun validateParentComment(parentId: Long?, postId: Long) {
        if (parentId == null) return
        val parent = commentRepo.findByIdAndNotDeleted(parentId)
            ?: throw CommentNotFoundException(ErrorCodes.PARENT_COMMENT_NOT_FOUND)

        if (parent.postId != postId) throw ValidationException(ErrorCodes.VALIDATION_EXCEPTION, "Post mismatch")
        if (parent.parentId != null) throw ValidationException(
            ErrorCodes.VALIDATION_EXCEPTION,
            "Max nesting level reached"
        )
    }

    private fun createDefaultStats(c: Comment) = CommentStats(
        c.userId,
        "Unknown",
        c.postId,
        c.id!!,
        c.content)

    private fun mapToDto(c: Comment, s: CommentStats) = CommentResponseDto(
        c.id!!,
        c.userId,
        c.postId,
        c.parentId,
        c.content,
        c.createdAt,
        s.hasReplies,
        s.likeCount
    )
}