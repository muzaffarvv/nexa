package uz.vv.reactionservice

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikeService(
    private val likeRepo: LikeRepo,
    private val aggregateRepo: LikeAggregateRepo
) {

    @Transactional
    fun toggleLike(userId: Long, targetType: ReactionTargetType, targetId: Long): Boolean {
        return try {
            val existingLike = likeRepo.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)

            if (existingLike == null) {
                likeRepo.save(Like(userId = userId, targetType = targetType, targetId = targetId))
                incrementCount(targetType, targetId)
                true
            } else {
                likeRepo.delete(existingLike)
                decrementCount(targetType, targetId)
                false
            }
        } catch (e: Exception) {
            throw InternalServerException(message = "Could not process like: ${e.message}")
        }
    }

    @Transactional(readOnly = true)
    fun getLikeCount(targetType: ReactionTargetType, targetId: Long): Long {
        return aggregateRepo.findByTargetTypeAndTargetId(targetType, targetId)?.likeCount ?: 0
    }

    private fun incrementCount(type: ReactionTargetType, id: Long) {
        ensureAggregateExists(type, id)
        aggregateRepo.updateCount(type, id, 1)
    }

    private fun decrementCount(type: ReactionTargetType, id: Long) {
        val aggregate = aggregateRepo.findByTargetTypeAndTargetId(type, id)
            ?: throw ReactionNotFoundException(message = "Like aggregate not found for $type:$id")

        if (aggregate.likeCount > 0) {
            aggregateRepo.updateCount(type, id, -1)
        }
    }

    private fun ensureAggregateExists(type: ReactionTargetType, id: Long) {
        aggregateRepo.initAggregate(type.name, id)
    }
}