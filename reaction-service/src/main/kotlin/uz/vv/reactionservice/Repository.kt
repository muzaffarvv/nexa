package uz.vv.reactionservice

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface LikeRepo : JpaRepository<Like, Long> {
    fun findByUserIdAndTargetTypeAndTargetId(userId: Long, targetType: ReactionTargetType, targetId: Long): Like?
}

@Repository
interface LikeAggregateRepo : JpaRepository<LikeAggregate, Long> {
    fun findByTargetTypeAndTargetId(targetType: ReactionTargetType, targetId: Long): LikeAggregate?

    @Modifying
    @Query("UPDATE LikeAggregate l SET l.likeCount = l.likeCount + :delta WHERE l.targetType = :type AND l.targetId = :id")
    fun updateCount(type: ReactionTargetType, id: Long, delta: Long)

    @Modifying
    @Query(value = """
        INSERT INTO like_aggregates (target_type, target_id, like_count)
        VALUES (:type, :id, 0)
        ON CONFLICT (target_type, target_id) DO NOTHING
    """, nativeQuery = true)
    fun initAggregate(type: String, id: Long)
}