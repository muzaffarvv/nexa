package uz.vv.commentservice

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "user-service", path = "/api/v1/users")
interface UserFeignClient {

    @GetMapping("/{id}")
    fun getUserById(@PathVariable("id") id: Long): UserResponseDto
}

data class UserResponseDto(
    val id: Long,
    val username: String
)

@FeignClient(name = "reaction-service", path = "/api/v1/reactions")
interface ReactionFeignClient {

    @PostMapping("/toggle")
    fun toggleLike(
        @RequestParam("userId") userId: Long,
        @RequestParam("targetType") targetType: ReactionTargetType,
        @RequestParam("targetId") targetId: Long
    ): Boolean
}

enum class ReactionTargetType {
    COMMENT
}