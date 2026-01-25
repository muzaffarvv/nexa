package uz.vv.userrelationservice

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface UserRefRepository : JpaRepository<UserRef, Long> {

}


@Repository
interface FollowRepository : JpaRepository<Follow, Long> {

    // Ikki foydalanuvchi o'rtasidagi aloqani tekshirish
    fun findByFollowerIdAndFollowingIdAndDeletedFalse(followerId: Long, followingId: Long): Follow?

    fun existsByFollowerIdAndFollowingIdAndDeletedFalse(followerId: Long, followingId: Long): Boolean

    // Foydalanuvchining obunachilarini olish (Followers)
    fun findAllByFollowingIdAndDeletedFalse(followingId: Long, pageable: Pageable): Page<Follow>

    // Foydalanuvchi kimlarga obuna bo'lganini olish (Following)
    fun findAllByFollowerIdAndDeletedFalse(followerId: Long, pageable: Pageable): Page<Follow>

    // Sanash uchun (Statistika yangilashda kerak bo'ladi)
    fun countByFollowingIdAndDeletedFalse(followingId: Long): Long
    fun countByFollowerIdAndDeletedFalse(followerId: Long): Long
}

@Repository
interface UserStatsRepository : JpaRepository<UserStats, Long> {

    fun findByUserIdAndDeletedFalse(userId: Long): UserStats?

    fun findByUsernameAndDeletedFalse(username: String): UserStats?

    // Bir nechta userlar statistikasini olish (masalan, feed ro'yxatida ismlarni ko'rsatish uchun)
    fun findAllByUserIdInAndDeletedFalse(userIds: List<Long>): List<UserStats>
}

@Repository
interface UserBlockRepository : JpaRepository<UserBlock, Long> {

    fun existsByBlockerIdAndBlockedIdAndDeletedFalse(blockerId: Long, blockedId: Long): Boolean

    fun findByBlockerIdAndBlockedIdAndDeletedFalse(
        blockerId: Long,
        blockedId: Long
    ): UserBlock?

    // Foydalanuvchi kimlarni bloklaganini ko'rish
    fun findAllByBlockerIdAndDeletedFalse(blockerId: Long, pageable: Pageable): Page<UserBlock>

    // Check bidirectional blocking (useful for feed or messaging)
    @Query("""
        SELECT CASE WHEN COUNT(ub) > 0 THEN true ELSE false END
        FROM UserBlock ub
        WHERE ((ub.blocker.id = :userId1 AND ub.blocked.id = :userId2)
            OR (ub.blocker.id = :userId2 AND ub.blocked.id = :userId1))
        AND ub.deleted = false
    """)
    fun isBlockedBetween(
        @Param("userId1") userId1: Long,
        @Param("userId2") userId2: Long
    ): Boolean
}

@Repository
interface FollowRequestRepository : JpaRepository<FollowRequest, Long> {

    fun findByRequesterIdAndTargetIdAndDeletedFalse(requesterId: Long, targetId: Long): FollowRequest?

    // Foydalanuvchiga kelgan barcha so'rovlar
    fun findAllByTargetIdAndDeletedFalse(targetId: Long, pageable: Pageable): Page<FollowRequest>

    fun existsByRequesterIdAndTargetIdAndDeletedFalse(requesterId: Long, targetId: Long): Boolean
}