package uz.vv.mediaservice

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MediaRepo : JpaRepository<MediaFile, Long> {
    fun findAllByOwnerTypeAndOwnerIdAndDeletedFalse(
        ownerType: MediaOwnerType,
        ownerId: Long
    ): List<MediaFile>

    fun findByKeyName(keyName: String): MediaFile?
}