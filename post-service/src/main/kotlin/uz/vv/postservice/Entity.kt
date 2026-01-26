package uz.vv.postservice

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
    name = "posts",
    indexes = [
        Index(
            name = "idx_posts_user_created",
            columnList = "user_id, created_at DESC"
        ),
        Index(
            name = "idx_posts_parent_id",
            columnList = "parent_id"
        )
    ]
)
class Post(

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    // Thread / repost / reply
    @Column(name = "parent_id")
    val parentId: Long? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "media_url")
    var mediaUrl: String? = null,

    @Column(nullable = false)
    var archived: Boolean = false //

) : BaseEntity()

@Entity
@Table(name = "post_stats")
class PostStats(

    // user-required info (id, username)
    @Column(nullable = false)
    var userId: Long,

    @Column(nullable = false)
    var username: String,

    // post-required info
    @Column(nullable = false, unique = true)
    var postId: Long,

    @Column(nullable = false)
    var content: String,

    // agar postga media biriktirilgan bo‘lsa
    @Column
    var mediaUrl: String? = null,

    // agar subposts mavjud bo‘lsa
    @Column(nullable = false)
    var hasSubPosts: Boolean = false,

    // comment count
    @Column(nullable = false)
    var commentCount: Long = 0,

    // reaction / like count
    @Column(nullable = false)
    var likeCount: Long = 0

) : BaseEntity()

