package uz.vv.mediaservice

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = Instant.now()

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: Status = Status.ACTIVE

    @Column(nullable = false)
    var deleted: Boolean = false
}


@Entity
@Table(
    name = "media_files",
    indexes = [
        Index(
            name = "idx_media_owner",
            columnList = "owner_type, owner_id"
        ),
        Index(
            name = "idx_media_created",
            columnList = "created_at DESC"
        )
    ]
)
class MediaFile(

    // (POST / COMMENT / USER)
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    val ownerType: MediaOwnerType,

    // (postId / commentId / userId)
    @Column(name = "owner_id", nullable = false)
    val ownerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: MediaType,

    @Column(name = "original_name", nullable = false)
    val originalName: String,

    @Column(name = "storage_key", nullable = false, unique = true)
    val storageKey: String,

    @Column(nullable = false)
    val url: String,

    @Column(nullable = false)
    val size: Long,

    val width: Int? = null,
    val height: Int? = null,
) : BaseEntity()
