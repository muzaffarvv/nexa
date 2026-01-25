package uz.vv.commentservice

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/comments")
class CommentController(private val commentService: CommentService) {

    @PostMapping
    fun create(
        @Valid @RequestBody dto: CommentCreateDto,
        @RequestHeader("X-User-Id") currentUserId: Long
    ): CommentResponseDto {
        return commentService.create(dto, currentUserId)
    }

    @GetMapping("/post/{postId}")
    fun getPostComments(
        @PathVariable postId: Long,
        pageable: Pageable
    ): Page<CommentResponseDto> {
        return commentService.getPostComments(postId, pageable)
    }

    @GetMapping("/{parentId}/replies")
    fun getReplies(@PathVariable parentId: Long): List<CommentResponseDto> {
        return commentService.getReplies(parentId)
    }

    @PostMapping("/{id}/like")
    fun toggleLike(
        @PathVariable id: Long,
        @RequestHeader("X-User-Id") currentUserId: Long
    ): Boolean {
        return commentService.toggleLike(id, currentUserId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
        @RequestHeader("X-User-Id") currentUserId: Long
    ) {
        commentService.delete(id, currentUserId)
    }
}