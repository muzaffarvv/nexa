package uz.vv.mediaservice

import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/media")
class MediaController(private val mediaService: MediaService) {

    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("ownerType") ownerType: MediaOwnerType,
        @RequestParam("ownerId") ownerId: Long
    ): MediaFile {
        return mediaService.upload(file, ownerType, ownerId)
    }

    @GetMapping("/{ownerType}/{ownerId}")
    fun getMedia(
        @PathVariable ownerType: MediaOwnerType,
        @PathVariable ownerId: Long
    ): List<MediaFile> {
        return mediaService.getByOwner(ownerType, ownerId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        mediaService.delete(id)
    }
}