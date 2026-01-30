package uz.vv.mediaservice

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.SecureRandom

@Service
class MediaService(
    private val mediaRepo: MediaRepo,
    private val userServiceClient: UserServiceClient,
    private val userRelationServiceClient: UserRelationServiceClient,
    private val postServiceClient: PostServiceClient,
    private val commentServiceClient: CommentServiceClient,
    @Value("\${file.upload-dir:uploads}") private val uploadDir: String
) {

    init {
        Files.createDirectories(Paths.get(System.getProperty("user.home"), uploadDir))
    }

    @Transactional
    fun upload(file: MultipartFile, ownerType: MediaOwnerType, ownerId: Long): MediaFile {
        if (file.isEmpty) throw FileEmptyException()

        val originalName = getOriginalFileName(file)
        val storageKey = generateFileKey("key", ownerId)
        val keyWithExt = appendExtension(storageKey, originalName)

        val targetPath = saveFileToDisk(file, ownerType, ownerId, keyWithExt)

        val mediaFile = createMediaFile(file, ownerType, ownerId, originalName, keyWithExt, targetPath.toString())
        linkMediaToOwner(mediaFile)

        return mediaRepo.save(mediaFile)
    }

    private fun getOriginalFileName(file: MultipartFile): String {
        return file.originalFilename ?: "unnamed"
    }

    private fun appendExtension(storageKey: String, originalName: String): String {
        val extension = originalName.substringAfterLast('.', "")
        return if (extension.isNotEmpty()) "$storageKey.$extension" else storageKey
    }

    private fun saveFileToDisk(file: MultipartFile, ownerType: MediaOwnerType, ownerId: Long, fileName: String) =
        Paths.get(System.getProperty("user.home"), uploadDir)
            .resolve(Paths.get(ownerType.name.lowercase(), ownerId.toString()))
            .also { Files.createDirectories(it) }
            .resolve(fileName)
            .also { Files.copy(file.inputStream, it, StandardCopyOption.REPLACE_EXISTING) }

    private fun createMediaFile(
        file: MultipartFile,
        ownerType: MediaOwnerType,
        ownerId: Long,
        originalName: String,
        keyName: String,
        path: String
    ): MediaFile {
        return MediaFile(
            ownerType = ownerType,
            ownerId = ownerId,
            type = determineMediaType(file.contentType),
            orgName = originalName,
            keyName = keyName,
            url = path,
            size = file.size
        )
    }

    private fun linkMediaToOwner(mediaFile: MediaFile) {
        val request = MediaLinkRequest(mediaFile.keyName, mediaFile.ownerId)
        when (mediaFile.ownerType) {
            MediaOwnerType.USER -> {
                userServiceClient.linkMedia(request)
                userRelationServiceClient.attachMedia(request)
            }
            MediaOwnerType.POST -> postServiceClient.linkMedia(request)
            MediaOwnerType.COMMENT -> commentServiceClient.linkMedia(request)
        }
    }

    fun generateFileKey(prefix: String, ownerId: Long): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val rnd = SecureRandom()
        val randomPart = (1..8).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        return "$prefix-$ownerId-$randomPart"
    }

    private fun determineMediaType(contentType: String?): MediaType {
        return when {
            contentType?.startsWith("video") == true -> MediaType.VIDEO
            contentType?.contains("gif") == true -> MediaType.GIF
            else -> MediaType.IMAGE
        }
    }

    fun getByKeyName(keyName: String): MediaFile {
        return mediaRepo.findByKeyName(keyName) ?: throw FileNotFoundException(
            ErrorCodes.FILE_NOT_FOUND,
            "File not found: $keyName"
        )
    }

    fun getByOwner(ownerType: MediaOwnerType, ownerId: Long): List<MediaFile> {
        return mediaRepo.findAllByOwnerTypeAndOwnerIdAndDeletedFalse(ownerType, ownerId)
    }

    @Transactional
    fun delete(id: Long) {
        val media = mediaRepo.findById(id)
            .orElseThrow { FileNotFoundException(ErrorCodes.FILE_NOT_FOUND, "File not found: $id") }
        media.deleted = true
        mediaRepo.save(media)
        try {
            Files.deleteIfExists(Paths.get(media.url))
        } catch (e: Exception) {
            println("Error deleting from disk: ${e.message}")
        }
    }
}