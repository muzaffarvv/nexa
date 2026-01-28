package uz.vv.userrelationservice

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface UserRelationService {

    // Follow actions
    fun followUser(currentUserId: Long, targetUserId: Long): String
    fun unfollowUser(currentUserId: Long, targetUserId: Long)

    // Follow Request actions
    fun acceptFollowRequest(currentUserId: Long, requesterId: Long)
    fun rejectFollowRequest(currentUserId: Long, requesterId: Long)
    fun getPendingRequests(currentUserId: Long, pageable: Pageable): Page<FollowRequestDto>

    // Block actions
    fun blockUser(currentUserId: Long, targetUserId: Long)
    fun unblockUser(currentUserId: Long, targetUserId: Long)
    fun getBlockedUsers(currentUserId: Long, pageable: Pageable): Page<UserBlockDto>

    // Lists
    fun getFollowers(userId: Long, pageable: Pageable): Page<FollowDto>
    fun getFollowing(userId: Long, pageable: Pageable): Page<FollowDto>

    // Profile & Stats
    fun getUserProfile(currentUserId: Long, targetUserId: Long): UserProfileDto
}

@Service
class UserRelationServiceImpl(
    private val followRepository: FollowRepository,
    private val followRequestRepository: FollowRequestRepository,
    private val userBlockRepository: UserBlockRepository,
    private val userStatsRepository: UserStatsRepository,
    private val userRefRepository: UserRefRepository,
    private val userFeignClient: UserFeignClient,
    private val postFeignClient: PostFeignClient,
    private val mapper: Mapper
) : UserRelationService {

    @Transactional
    override fun followUser(currentUserId: Long, targetUserId: Long): String {
        validateSelfFollow(currentUserId, targetUserId)

        checkBlockRelationship(currentUserId, targetUserId)

        handleExistingFollow(currentUserId, targetUserId)?.let {
            return it
        }

        val targetUser = getTargetUserOrThrow(targetUserId)

        validateNoPendingRequest(currentUserId, targetUserId)

        return processFollowLogic(currentUserId, targetUserId, targetUser.isPrivate)
    }

    private fun validateSelfFollow(currentUserId: Long, targetUserId: Long) {
        if (currentUserId == targetUserId) {
            throw SelfActionNotAllowedException("You can't follow yourself")
        }
    }

    private fun handleExistingFollow(
        currentUserId: Long,
        targetUserId: Long
    ): String? {
        val follow = followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
            ?: return null

        if (!follow.deleted) {
            throw AlreadyFollowingException("Already following")
        }

        follow.deleted = false
        followRepository.save(follow)
        updateBothUserStats(currentUserId, targetUserId)
        return "Following"
    }

    private fun getTargetUserOrThrow(targetUserId: Long): UserResponseDto {
        return try {
            userFeignClient.getUserById(targetUserId)
        } catch (ex: Exception) {
            throw UserNotFoundException()
        }
    }

    private fun validateNoPendingRequest(currentUserId: Long, targetUserId: Long) {
        if (followRequestRepository
                .existsByRequesterIdAndTargetIdAndDeletedFalse(currentUserId, targetUserId)
        ) {
            throw FollowRequestExistsException("Request already sent")
        }
    }

    private fun processFollowLogic(
        currentUserId: Long,
        targetUserId: Long,
        isPrivate: Boolean
    ): String {
        return if (isPrivate) {
            createFollowRequest(currentUserId, targetUserId)
            "Requested"
        } else {
            createFollow(currentUserId, targetUserId)
            "Following"
        }
    }

    private fun updateBothUserStats(userId1: Long, userId2: Long) {
        updateUserStats(userId1)
        updateUserStats(userId2)
    }



    @Transactional
    override fun unfollowUser(currentUserId: Long, targetUserId: Long) {
        val follow = followRepository.findByFollowerIdAndFollowingIdAndDeletedFalse(currentUserId, targetUserId)
            ?: throw FollowRelationNotFoundException("Follow relationship not found")


        follow.deleted = true
        followRepository.save(follow)

        updateUserStats(currentUserId)
        updateUserStats(targetUserId)
    }

    @Transactional
    override fun acceptFollowRequest(currentUserId: Long, requesterId: Long) {
        val request = followRequestRepository.findByRequesterIdAndTargetIdAndDeletedFalse(requesterId, currentUserId)
            ?: throw FollowRelationNotFoundException("Request not found")


        request.deleted = true
        followRequestRepository.save(request)

        createFollow(requesterId, currentUserId)
    }

    @Transactional
    override fun rejectFollowRequest(currentUserId: Long, requesterId: Long) {
        val request = followRequestRepository.findByRequesterIdAndTargetIdAndDeletedFalse(requesterId, currentUserId)
            ?: throw FollowRelationNotFoundException("Request not found")

        request.deleted = true
        followRequestRepository.save(request)
    }

    @Transactional(readOnly = true)
    override fun getPendingRequests(currentUserId: Long, pageable: Pageable): Page<FollowRequestDto> {
        return followRequestRepository.findAllByTargetIdAndDeletedFalse(currentUserId, pageable)
            .map { mapper.run { it.toDto() } }
    }

    @Transactional
    override fun blockUser(currentUserId: Long, targetUserId: Long) {
        if (userBlockRepository.existsByBlockerIdAndBlockedIdAndDeletedFalse(currentUserId, targetUserId)) {
            return
        }

        unfollowIfExists(currentUserId, targetUserId)
        unfollowIfExists(targetUserId, currentUserId)

        val blocker = ensureUserRef(currentUserId)
        val blocked = ensureUserRef(targetUserId)

        val block = UserBlock().apply {
            this.blocker = blocker
            this.blocked = blocked
        }
        userBlockRepository.save(block)
    }

    @Transactional
    override fun unblockUser(currentUserId: Long, targetUserId: Long) {
        val block = userBlockRepository
            .findByBlockerIdAndBlockedIdAndDeletedFalse(currentUserId, targetUserId)
            ?: throw BlockRelationNotFoundException("Block not found")

        block.deleted = true
    }

    @Transactional(readOnly = true)
    override fun getBlockedUsers(currentUserId: Long, pageable: Pageable): Page<UserBlockDto> {
        return userBlockRepository.findAllByBlockerIdAndDeletedFalse(currentUserId, pageable)
            .map { mapper.run { it.toDto() } }
    }

    @Transactional(readOnly = true)
    override fun getFollowers(userId: Long, pageable: Pageable): Page<FollowDto> {
        return followRepository.findAllByFollowingIdAndDeletedFalse(userId, pageable)
            .map { mapper.run { it.toDto() } }
    }

    @Transactional(readOnly = true)
    override fun getFollowing(userId: Long, pageable: Pageable): Page<FollowDto> {
        return followRepository.findAllByFollowerIdAndDeletedFalse(userId, pageable)
            .map { mapper.run { it.toDto() } }
    }

    @Transactional
    override fun getUserProfile(currentUserId: Long, targetUserId: Long): UserProfileDto {
        val isBlockedByMe = userBlockRepository.existsByBlockerIdAndBlockedIdAndDeletedFalse(currentUserId, targetUserId)
        val isBlockedByTarget = userBlockRepository.existsByBlockerIdAndBlockedIdAndDeletedFalse(targetUserId, currentUserId)

        if (isBlockedByTarget) {
            throw UserNotFoundException() // Bloklagan odamga user ko'rinmaydi
        }

        val stats = userStatsRepository.findByUserIdAndDeletedFalse(targetUserId)
            ?: syncUserStatsFromFeign(targetUserId)

        val postsCount = try {
            postFeignClient.getUserPostsCount(targetUserId)
        } catch (e: Exception) {
            0L
        }

        val isFollowing = followRepository.existsByFollowerIdAndFollowingIdAndDeletedFalse(currentUserId, targetUserId)

        return mapper.run {
            stats.toProfileDto(
                postsCount = if (stats.isPrivate && !isFollowing && currentUserId != targetUserId) 0 else postsCount,
                isFollowing = isFollowing,
                isBlocked = isBlockedByMe
            )
        }
    }

    // --- Private Helpers ---

    private fun createFollow(followerId: Long, followingId: Long) {
        val followerRef = ensureUserRef(followerId)
        val followingRef = ensureUserRef(followingId)

        val follow = Follow().apply {
            this.follower = followerRef
            this.following = followingRef
        }
        followRepository.save(follow)

        updateUserStats(followerId)
        updateUserStats(followingId)
    }

    private fun createFollowRequest(requesterId: Long, targetId: Long) {
        val request = FollowRequest().apply {
            this.requesterId = requesterId
            this.targetId = targetId
        }
        followRequestRepository.save(request)
    }

    private fun checkBlockRelationship(u1: Long, u2: Long) {
        val hasBlock = userBlockRepository.isBlockedBetween(u1, u2)
        if (hasBlock) {
            throw ActionNotAllowedException("Action not allowed due to block")
        }
    }

    private fun unfollowIfExists(followerId: Long, followingId: Long) {
        val follow = followRepository.findByFollowerIdAndFollowingIdAndDeletedFalse(followerId, followingId)
        if (follow != null) {
            follow.deleted = true
            followRepository.save(follow)
            updateUserStats(followerId)
            updateUserStats(followingId)
        }
    }

    private fun updateUserStats(userId: Long) {
        val stats = userStatsRepository.findByUserIdAndDeletedFalse(userId)
            ?: syncUserStatsFromFeign(userId)

        val followers = followRepository.countByFollowingIdAndDeletedFalse(userId)
        val following = followRepository.countByFollowerIdAndDeletedFalse(userId)

        stats.followersCount = followers
        stats.followingCount = following
        userStatsRepository.save(stats)
    }

    private fun syncUserStatsFromFeign(userId: Long): UserStats {
        val userDto = userFeignClient.getUserById(userId)
        val newStats = UserStats(
            userId = userDto.id,
            fullName = userDto.fullName ?: "",
            username = userDto.username,
            bio = userDto.bio,
            profileImageUrl = userDto.profileImageUrl,
            isPrivate = userDto.isPrivate
        )
        return userStatsRepository.save(newStats)
    }

    // UserRef jadvalida ID borligini ta'minlash
    private fun ensureUserRef(userId: Long): UserRef {
        return userRefRepository.findById(userId).orElseGet {
            val ref = UserRef()
            ref.id = userId
            userRefRepository.save(ref)
        }
    }
}