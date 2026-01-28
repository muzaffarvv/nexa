package uz.vv.postservice

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "user-service", path = "/api/v1/users")
interface UserFeignClient {

    @GetMapping("/{id}")
    fun getUserById(@PathVariable("id") id: Long): UserResponseDto

    @PostMapping("/batch")
    fun getUsersByIds(@RequestParam("ids") ids: List<Long>): List<UserResponseDto>
}

data class UserResponseDto(
    val id: Long,
    val username: String,
    val fullName: String? = null
)

@FeignClient(name = "reaction-service", path = "/api/v1/reactions")
interface ReactionFeignClient {

    @PostMapping("/toggle")
    fun toggleLike(
        @RequestParam("userId") userId: Long,
        @RequestParam("targetType") targetType: ReactionTargetType,
        @RequestParam("targetId") targetId: Long
    ): Boolean

    @GetMapping("/count")
    fun getReactionCount(
        @RequestParam("targetType") targetType: ReactionTargetType,
        @RequestParam("targetId") targetId: Long
    ): Long

    @PostMapping("/batch/count")
    fun getReactionCounts(
        @RequestParam("targetType") targetType: ReactionTargetType,
        @RequestParam("targetIds") targetIds: List<Long>
    ): Map<Long, Long>
}

enum class ReactionTargetType {
    POST,
    COMMENT
}

@FeignClient(name = "comment-service", path = "/api/v1/comments")
interface CommentFeignClient {

    @GetMapping("/count/post/{postId}")
    fun getCommentCount(@PathVariable("postId") postId: Long): Long
}

// feign media