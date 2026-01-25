package uz.vv.userservice

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "media-service", path = "/api/v1/media")
interface MediaFeignClient {

    @GetMapping("/{ownerType}/{ownerId}")
    fun getMediaByOwner(
        @PathVariable("ownerType") ownerType: String,
        @PathVariable("ownerId") ownerId: Long
    ): List<MediaFileDto>
}

data class MediaFileDto(
    val id: Long,
    val ownerType: String,
    val ownerId: Long,
    val type: String,
    val originalName: String,
    val storageKey: String,
    val url: String,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null
)