package uz.vv.reactionservice

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/reactions")
class LikeController(
    private val likeService: LikeService
) {

    @PostMapping("/toggle")
    fun toggleLike(
        @RequestParam userId: Long,
        @RequestParam targetType: ReactionTargetType,
        @RequestParam targetId: Long
    ): Boolean {
        return likeService.toggleLike(userId, targetType, targetId)
    }

    @GetMapping("/count/{targetType}/{targetId}")
    fun getLikeCount(
        @PathVariable targetType: ReactionTargetType,
        @PathVariable targetId: Long
    ): Long {
        return likeService.getLikeCount(targetType, targetId)
    }
}