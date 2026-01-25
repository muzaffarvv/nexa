package uz.vv.commentservice

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
    var id: Long ? = null

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
    name = "comments", indexes = [Index(
        name = "idx_comments_post_created", columnList = "post_id, created_at DESC"
    ), Index(
        name = "idx_comments_parent", columnList = "parent_id"
    ), Index(
        name = "idx_comments_user", columnList = "user_id"
    )]
)
class Comment(

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    // Qaysi postga tegishli
    @Column(name = "post_id", nullable = false)
    val postId: Long,

    // Reply bo‘lsa → boshqa comment ID
    @Column(name = "parent_id")
    val parentId: Long? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    ) : BaseEntity()

@Entity
@Table(name = "comment_stats")
class CommentStats(

    // user-required info
    @Column(nullable = false)
    var userId: Long,

    @Column(nullable = false)
    var username: String,

    // comment info
    @Column(nullable = false)
    var postId: Long,

    @Column(nullable = false)
    var commentId: Long,

    @Column()
    var content: String,

    @Column
    var mediaUrl: String? = null,

    @Column(nullable = false)
    var hasReplies: Boolean = false,

    @Column(nullable = false)
    var likeCount: Long = 0

) : BaseEntity()

/*
CREATE INDEX idx_comment_stats_comment ON comment_stats(comment_id);
CREATE INDEX idx_comment_stats_post ON comment_stats(post_id);
CREATE INDEX idx_comment_stats_user ON comment_stats(user_id);
*/



