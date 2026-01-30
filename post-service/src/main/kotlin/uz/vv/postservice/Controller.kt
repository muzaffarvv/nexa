package uz.vv.postservice

import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Valid @RequestBody dto: PostCreateDto,
        @RequestHeader("X-User-Id") currentUserId: Long
    ): PostResponseDto =
        postService.create(dto, currentUserId)

    @GetMapping("/{id}")
    fun getPost(@PathVariable id: Long): PostResponseDto =
        postService.getById(id)

    @GetMapping
    fun listPosts(
        @RequestParam(defaultValue = "false") archived: Boolean,
        @PageableDefault(
            size = 20,
            sort = ["createdAt"],
            direction = Sort.Direction.DESC
        ) pageable: Pageable
    ): Page<PostResponseDto> =
        postService.listPosts(archived, pageable)

    @GetMapping("/user/{userId}/count")
    fun getPostCount(@PathVariable userId: Long): Int = postService.postCountByUserId(userId)

    @GetMapping("/user/{userId}")
    fun getUserPosts(
        @PathVariable userId: Long,
        @PageableDefault(
            size = 20,
            sort = ["createdAt"],
            direction = Sort.Direction.DESC
        ) pageable: Pageable
    ): Page<PostResponseDto> =
        postService.getByUserId(userId, pageable)

    @GetMapping("/{id}/replies")
    fun getReplies(
        @PathVariable id: Long,
        @PageableDefault(
            size = 20,
            sort = ["createdAt"],
            direction = Sort.Direction.ASC
        ) pageable: Pageable
    ): Page<PostResponseDto> =
        postService.getReplies(id, pageable)

    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @Valid @RequestBody dto: PostUpdateDto,
        @RequestHeader("X-User-Id") currentUserId: Long
    ): PostResponseDto =
        postService.update(id, dto, currentUserId)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable id: Long,
        @RequestHeader("X-User-Id") currentUserId: Long
    ) {
        postService.delete(id, currentUserId)
    }

    @PostMapping("/{id}/like")
    fun toggleLike(
        @PathVariable id: Long,
        @RequestHeader("X-User-Id") currentUserId: Long
    ): LikeResponseDto =
        postService.toggleLike(id, currentUserId)

    @GetMapping("/{id}/stats")
    fun getPostStats(@PathVariable id: Long): PostStatsResponseDto =
        postService.getPostStats(id)

    @PostMapping("/internal/media/link")
    fun linkMedia(@RequestBody request: MediaLinkRequest) {
        postService.updateMediaLink(request.ownerId, request.mediaKey)
    }

}
