package uz.vv.userrelationservice

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    var id: Long? = null

    @CreatedDate
    @Column(updatable = false, nullable = false)
    var createdAt: Instant? = Instant.now()

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = Instant.now()

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: Status = Status.ACTIVE

    @Column(nullable = false)
    var deleted: Boolean = false
}

@Entity
@Table(name = "users")
class UserRef {

    @Id
    var id: Long? = null
} // yangi user yatilganda userni IDsi bunga saqlab qo'yiladi


@Entity
@Table(
    name = "user_follow",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["follower_id", "following_id"])
    ],
    indexes = [
        Index(name = "idx_follow_follower", columnList = "follower_id"),
        Index(name = "idx_follow_following", columnList = "following_id")
    ]
)
class Follow : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    lateinit var follower: UserRef

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id", nullable = false)
    lateinit var following: UserRef
}

@Entity
@Table(name = "user_stats", indexes = [
    Index(name = "idx_user_stats_user_id", columnList = "user_id"), // userId bo'yicha tez qidirish uchun
    Index(name = "idx_user_stats_followers", columnList = "followers_count"),
    Index(name = "idx_user_stats_following", columnList = "following_count")
])
class UserStats(

    @Column(name = "user_id", unique = true, nullable = false)
    var userId: Long? = null,

    // Profil ma'lumotlari
    @Column(nullable = false, length = 100)
    var fullName: String = "",

    @Column(nullable = false, length = 30)
    var username: String = "",

    @Column(length = 150)
    var bio: String? = null,

    @Column(name = "media_key", unique = true)
    var mediaKey: String? = null,

    @Column(name = "is_private", nullable = false)
    var isPrivate: Boolean = false,

    // Aggregated counts
    @Column(name = "followers_count", nullable = false)
    var followersCount: Long = 0,

    @Column(name = "following_count", nullable = false)
    var followingCount: Long = 0

) : BaseEntity()

/*
  CREATE INDEX idx_follow_follower ON user_follow(follower_id);
  CREATE INDEX idx_follow_following ON user_follow(following_id);

  CREATE INDEX idx_block_blocker ON user_block(blocker_id);
  CREATE INDEX idx_block_blocked ON user_block(blocked_id);
 */

@Entity
@Table(
    name = "user_block",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["blocker_id", "blocked_id"])
    ],
    indexes = [
        Index(name = "idx_block_blocker", columnList = "blocker_id"),
        Index(name = "idx_block_blocked", columnList = "blocked_id")
    ]
)
class UserBlock : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    lateinit var blocker: UserRef

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    lateinit var blocked: UserRef
}


@Entity
@Table(
    name = "follow_request",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["requester_id", "target_id"])
    ]
)
class FollowRequest : BaseEntity() {  //  isPrivate = true

    var requesterId: Long? = null
    var targetId: Long? = null
}

