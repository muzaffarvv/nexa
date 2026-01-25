package uz.vv.reactionservice

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    var id: Long? = null
}

@Entity
@Table(
    name = "likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_target_like",
            columnNames = ["user_id", "target_type", "target_id"]
        )
    ]
)
data class Like(

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    // post or comment
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    val targetType: ReactionTargetType,

    // post or comment ID
    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "liked", nullable = false)
    var liked: Boolean = true, // for auditing

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant? = null
): BaseEntity()

@Entity
@Table(
    name = "like_aggregates",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_target_like",
            columnNames = ["target_type", "target_id"]
        )
    ]
)
data class LikeAggregate(

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    val targetType: ReactionTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0

): BaseEntity()

