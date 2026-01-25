package uz.vv.userrelationservice

import org.springframework.stereotype.Component

@Component
class Mapper {

    fun UserStats.toProfileDto(
        postsCount: Long,
        isFollowing: Boolean,
        isBlocked: Boolean
    ) = UserProfileDto(
        id = this.userId!!,
        username = this.username,
        fullName = this.fullName,
        bio = this.bio,
        profileImageUrl = this.profileImageUrl,
        isPrivate = this.isPrivate,
        postsCount = postsCount,
        followersCount = this.followersCount,
        followingCount = this.followingCount,
        isFollowing = isFollowing,
        isBlocked = isBlocked
    )

    fun Follow.toDto() = FollowDto(
        followerId = this.follower.id!!,
        followingId = this.following.id!!,
        createdAt = this.createdAt!!
    )

    fun UserBlock.toDto() = UserBlockDto(
        blockerId = this.blocker.id!!,
        blockedId = this.blocked.id!!,
        createdAt = this.createdAt!!
    )

    fun FollowRequest.toDto() = FollowRequestDto(
        requesterId = this.requesterId!!,
        targetId = this.targetId!!,
        createdAt = this.createdAt!!,
        status = this.status.name
    )

}