package uz.vv.mediaservice

import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.HttpStatus

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

    @GetMapping("/owner/{ownerType}/{ownerId}")
    fun getMedia(
        @PathVariable ownerType: MediaOwnerType,
        @PathVariable ownerId: Long
    ): List<MediaFile> {
        return mediaService.getByOwner(ownerType, ownerId)
    }

    @GetMapping("/key/{keyName}")
    fun getMediaByKeyName(@PathVariable keyName: String): MediaFile = mediaService.getByKeyName(keyName)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        mediaService.delete(id)
    }
}