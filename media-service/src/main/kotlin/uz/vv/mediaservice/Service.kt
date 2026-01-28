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
    @Value("\${file.upload-dir:uploads}") private val uploadDir: String
) {

    init {
        Files.createDirectories(Paths.get(System.getProperty("user.home"), uploadDir))
    }

    @Transactional
    fun upload(
        file: MultipartFile,
        ownerType: MediaOwnerType,
        ownerId: Long
    ): MediaFile {
        if (file.isEmpty) throw FileEmptyException()

        try {
            val originalName = file.originalFilename ?: "unnamed"

            val storageKey = generateFileKey("key", ownerId)

            val extension = originalName.substringAfterLast('.', "")
            val storageKeyWithExt = if (extension.isNotEmpty()) "$storageKey.$extension" else storageKey

            val relativePath = Paths.get(ownerType.name.lowercase(), ownerId.toString())
            val targetDir = Paths.get(System.getProperty("user.home"), uploadDir).resolve(relativePath)
            Files.createDirectories(targetDir)

            val targetPath = targetDir.resolve(storageKeyWithExt)
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            val mediaFile = MediaFile(
                ownerType = ownerType,
                ownerId = ownerId,
                type = determineMediaType(file.contentType),
                orgName = originalName,
                keyName = storageKeyWithExt,
                url = targetPath.toString(),
                size = file.size
            )

            return mediaRepo.save(mediaFile)
        } catch (e: Exception) {
            throw FileUploadFailedException(message = e.message)
        }
    }

    fun generateFileKey(prefix: String, ownerId: Long): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val rnd = SecureRandom()
        val randomPart = (1..8).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        return "$prefix-$ownerId-$randomPart"
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

    private fun determineMediaType(contentType: String?): MediaType {
        return when {
            contentType?.startsWith("video") == true -> MediaType.VIDEO
            contentType?.contains("gif") == true -> MediaType.GIF
            else -> MediaType.IMAGE
        }
    }
}