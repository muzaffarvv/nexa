package uz.vv.userservice

import jakarta.persistence.*
import jakarta.validation.constraints.Min
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
class User(
    @Column(length = 100)
    var fullName: String? = null,

    @Column(nullable = false, unique = true, length = 30)
    var username: String,

    @Column(nullable = true, length = 15)
    var phoneNumber: String? = null,

    @Column(length = 150)
    var bio: String? = null,

    @Column(length = 32, unique = true)
    var mediaKey: String? = null,

    @Min(13)
    @Column(nullable = true)
    var age: Int? = null,

    @Column(nullable = false)
    var isPrivate: Boolean = false
) : BaseEntity()

@Entity
@Table(name = "user_auth")
@EntityListeners(AuditingEntityListener::class)
class UserAuth (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,

    @Column(nullable = false, length = 72)
    var passwordHash: String,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)

// login: username -> id , id -> password

/* for performance
  CREATE UNIQUE INDEX ux_user_auth_user_id ON user_auth(user_id);
*/