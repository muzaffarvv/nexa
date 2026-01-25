package uz.vv.mediaservice

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

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
            val shortId = UUID.randomUUID().toString().replace("-", "").take(12)
            val storageKey = "${shortId}_$originalName"

            // Create folder hierarchy (by ownerType/ownerId)
            val relativePath = Paths.get(ownerType.name.lowercase(), ownerId.toString())
            val targetDir = Paths.get(System.getProperty("user.home"), uploadDir).resolve(relativePath)
            Files.createDirectories(targetDir)

            // Save file
            val targetPath = targetDir.resolve(storageKey)
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            // Save to database
            val mediaFile = MediaFile(
                ownerType = ownerType,
                ownerId = ownerId,
                type = determineMediaType(file.contentType),
                originalName = originalName,
                storageKey = storageKey,
                url = targetPath.toString(),
                size = file.size
            )

            return mediaRepo.save(mediaFile)
        } catch (e: Exception) {
            throw FileUploadFailedException(message = e.message)
        }
    }

    fun getByOwner(ownerType: MediaOwnerType, ownerId: Long): List<MediaFile> {
        return mediaRepo.findAllByOwnerTypeAndOwnerIdAndDeletedFalse(ownerType, ownerId)
    }

    @Transactional
    fun delete(id: Long) {
        val media = mediaRepo.findById(id).orElseThrow { FileNotFoundException() }
        media.deleted = true
        mediaRepo.save(media)

        // Delete file from disk (optional, soft delete is usually recommended)
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