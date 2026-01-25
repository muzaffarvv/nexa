package uz.vv.userrelationservice

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "user-service")
interface UserFeignClient {

    @GetMapping("/api/v1/users/{id}")
    fun getUserById(@PathVariable id: Long): UserResponseDto
}

data class UserResponseDto(
    val id: Long,
    val fullName: String?,
    val username: String,
    val phoneNumber: String?,
    val bio: String?,
    val profileImageUrl: String,
    val age: Int?,
    val isPrivate: Boolean
)

@FeignClient(name = "post-service")
interface PostFeignClient {

    @GetMapping("/api/v1/posts/user/{userId}/count")
    fun getUserPostsCount(@PathVariable userId: Long): Long
}
