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
        if (currentUserId == targetUserId)
            throw SelfActionNotAllowedException("You can't follow yourself")


        // 1. Block tekshirish (ikki tomonlama)
        checkBlockRelationship(currentUserId, targetUserId)

        // 2. Allaqachon follow bo'lganmi?
        if (followRepository.existsByFollowerIdAndFollowingIdAndDeletedFalse(currentUserId, targetUserId)) {
            throw AlreadyFollowingException("Already following")
        }


        // 3. User ma'lumotlarini olish (User Service orqali)
        val targetUserDto = try {
            userFeignClient.getUserById(targetUserId)
        } catch (e: Exception) {
            throw UserNotFoundException()
        }

        // 4. Pending request borligini tekshirish
        if (followRequestRepository.existsByRequesterIdAndTargetIdAndDeletedFalse(currentUserId, targetUserId)) {
            throw FollowRequestExistsException("Request already sent")
        }

        // 5. Logic: Private vs Public
        if (targetUserDto.isPrivate) {
            createFollowRequest(currentUserId, targetUserId)
            return "Requested"
        } else {
            createFollow(currentUserId, targetUserId)
            return "Following"
        }
    }

    @Transactional
    override fun unfollowUser(currentUserId: Long, targetUserId: Long) {
        val follow = followRepository.findByFollowerIdAndFollowingIdAndDeletedFalse(currentUserId, targetUserId)
            ?: throw FollowRelationNotFoundException("Follow relationship not found")


        follow.deleted = true
        followRepository.save(follow)

        // Statistikani yangilash
        updateUserStats(currentUserId)
        updateUserStats(targetUserId)
    }

    @Transactional
    override fun acceptFollowRequest(currentUserId: Long, requesterId: Long) {
        val request = followRequestRepository.findByRequesterIdAndTargetIdAndDeletedFalse(requesterId, currentUserId)
            ?: throw FollowRelationNotFoundException("Request not found")


        // Requestni o'chirish (soft delete)
        request.deleted = true
        followRequestRepository.save(request)

        // Follow yaratish
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

        // Agar ular o'rtasida follow bo'lsa, uni o'chiramiz
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
        // 1. Block tekshirish
        val isBlockedByMe = userBlockRepository.existsByBlockerIdAndBlockedIdAndDeletedFalse(currentUserId, targetUserId)
        val isBlockedByTarget = userBlockRepository.existsByBlockerIdAndBlockedIdAndDeletedFalse(targetUserId, currentUserId)

        if (isBlockedByTarget) {
            throw UserNotFoundException() // Bloklagan odamga user ko'rinmaydi
        }

        // 2. Stats olish (yoki yaratish)
        val stats = userStatsRepository.findByUserIdAndDeletedFalse(targetUserId)
            ?: syncUserStatsFromFeign(targetUserId)

        // 3. Postlar soni (Post Service)
        val postsCount = try {
            postFeignClient.getUserPostsCount(targetUserId)
        } catch (e: Exception) {
            0L
        }

        // 4. Munosabat
        val isFollowing = followRepository.existsByFollowerIdAndFollowingIdAndDeletedFalse(currentUserId, targetUserId)

        // 5. Private profil logikasi
        // Agar user private bo'lsa VA men follow qilmagan bo'lsam VA bu men o'zim bo'lmasam -> Postlar 0 ko'rinishi kerak (yoki null)
        // Mapper funksiyasi bizga tayyor DTO qaytaradi.

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