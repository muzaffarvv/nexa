package uz.vv.userrelationservice

import java.time.Instant

data class UserProfileDto(
    val id: Long,

    val username: String,
    val fullName: String,
    val bio: String?,
    val mediaKey: String?,
    val isPrivate: Boolean,

    val postsCount: Long,
    val followersCount: Long,
    val followingCount: Long,

    val isFollowing: Boolean,
    val isBlocked: Boolean
)

data class UserDto(
    val id: Long,
    val isPrivate: Boolean
)

data class FollowDto(
    val followerId: Long,
    val followingId: Long,
    val createdAt: Instant
)

data class UserBlockDto(
    val blockerId: Long,
    val blockedId: Long,
    val createdAt: Instant
)

data class FollowRequestDto(
    val requesterId: Long,
    val targetId: Long,
    val createdAt: Instant,
    val status: String // ACTIVE, REJECTED,
)

data class MediaLinkRequest(
    val mediaKey: String,
    val ownerId: Long
)

