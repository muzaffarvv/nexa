package uz.vv.userrelationservice

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user-relations")
class UserRelationController(
    private val userRelationService: UserRelationService
) {

    @PostMapping("/follow/{targetUserId}")
    fun followUser(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PathVariable targetUserId: Long
    ): String {
        return userRelationService.followUser(currentUserId, targetUserId)
    }

    @PostMapping("/unfollow/{targetUserId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun unfollowUser(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PathVariable targetUserId: Long
    ) {
        userRelationService.unfollowUser(currentUserId, targetUserId)
    }

    @PostMapping("/requests/{requesterId}/accept")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun acceptRequest(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PathVariable requesterId: Long
    ) {
        userRelationService.acceptFollowRequest(currentUserId, requesterId)
    }

    @PostMapping("/requests/{requesterId}/reject")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun rejectRequest(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PathVariable requesterId: Long
    ) {
        userRelationService.rejectFollowRequest(currentUserId, requesterId)
    }

    @GetMapping("/requests/pending")
    fun getPendingRequests(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<FollowRequestDto> {
        return userRelationService.getPendingRequests(currentUserId, pageable)
    }

    @PostMapping("/block/{targetUserId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun blockUser(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PathVariable targetUserId: Long
    ) {
        userRelationService.blockUser(currentUserId, targetUserId)
    }

    @PostMapping("/unblock/{targetUserId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun unblockUser(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PathVariable targetUserId: Long
    ) {
        userRelationService.unblockUser(currentUserId, targetUserId)
    }

    @GetMapping("/blocked")
    fun getBlockedUsers(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<UserBlockDto> {
        return userRelationService.getBlockedUsers(currentUserId, pageable)
    }

    @GetMapping("/{userId}/followers")
    fun getFollowers(
        @PathVariable userId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<FollowDto> {
        return userRelationService.getFollowers(userId, pageable)
    }

    @GetMapping("/{userId}/following")
    fun getFollowing(
        @PathVariable userId: Long,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<FollowDto> {
        return userRelationService.getFollowing(userId, pageable)
    }

    @GetMapping("/profile/{targetUserId}")
    fun getUserProfile(
        @RequestHeader("X-User-Id") currentUserId: Long,
        @PathVariable targetUserId: Long
    ): UserProfileDto {
        return userRelationService.getUserProfile(currentUserId, targetUserId)
    }
}