package uz.vv.mediaservice

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.RequestBody

data class MediaLinkRequest(
    val mediaKey: String,
    val ownerId: Long
)

@FeignClient(name = "user-service")
interface UserServiceClient {
    @PostMapping("/api/v1/users/internal/media/link")
    fun linkMedia(@RequestBody request: MediaLinkRequest)
}

@FeignClient(name = "user-relation-service")
interface UserRelationServiceClient {
    @PostMapping("/api/v1/user-relation/internal/media/attach")
    fun attachMedia(@RequestBody request: MediaLinkRequest)
}

@FeignClient(name = "post-service")
interface PostServiceClient {
    @PostMapping("/api/v1/posts/internal/media/link")
    fun linkMedia(@RequestBody request: MediaLinkRequest)
}

@FeignClient(name = "comment-service")
interface CommentServiceClient {
    @PostMapping("/api/v1/comments/internal/media/link")
    fun linkMedia(@RequestBody request: MediaLinkRequest)
}