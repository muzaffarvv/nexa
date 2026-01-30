package uz.vv.postservice

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import feign.FeignException

interface PostService {

    fun create(dto: PostCreateDto, currentUserId: Long): PostResponseDto
    fun getById(id: Long): PostResponseDto
    fun listPosts(archived: Boolean, pageable: Pageable): Page<PostResponseDto>
    fun getByUserId(userId: Long, pageable: Pageable): Page<PostResponseDto>
    fun getReplies(parentId: Long, pageable: Pageable): Page<PostResponseDto>
    fun update(id: Long, dto: PostUpdateDto, currentUserId: Long): PostResponseDto
    fun delete(id: Long, currentUserId: Long)
    fun toggleLike(postId: Long, userId: Long): LikeResponseDto
    fun getPostStats(postId: Long): PostStatsResponseDto
    fun postCountByUserId(userId: Long): Int
    fun updateMediaLink(ownerId: Long, mediaKey: String)
}

@Service
class PostServiceImpl(
    private val postRepo: PostRepo,
    private val postStatsRepo: PostStatsRepo,
    private val userFeignClient: UserFeignClient,
    private val reactionFeignClient: ReactionFeignClient,
    private val commentFeignClient: CommentFeignClient,
) : PostService {

    @Transactional
    override fun create(dto: PostCreateDto, currentUserId: Long): PostResponseDto {

        if (dto.userId != currentUserId) {
            throw AccessDeniedException("User mismatch")
        }

        dto.parentId?.let { parentId ->
            postRepo.findByIdAndDeletedFalse(parentId)
                ?: throw ParentPostNotFoundException("Parent post not found with id: $parentId")

            postStatsRepo.findByPostIdAndDeletedFalse(parentId).apply {
                hasSubPosts = true
                commentCount++
                postStatsRepo.save(this)
            }
        }

        val user = fetchUser(dto.userId)
        val post = savePostEntity(dto)
        savePostStats(post, user)

        return toResponse(post)
    }

    @Transactional
    override fun updateMediaLink(ownerId: Long, mediaKey: String) {
        val post = postRepo.findByIdAndDeletedFalse(ownerId)
            ?: throw PostNotFoundException("Post not found with id: $ownerId")

        val postStats = postStatsRepo.findByPostIdAndDeletedFalse(ownerId)

        post.mediaKey = mediaKey
        postStats.mediaKey = mediaKey

        postRepo.save(post)
        postStatsRepo.save(postStats)
    }

    private fun savePostEntity(dto: PostCreateDto): Post {
        val post = Post(
            userId = dto.userId,
            parentId = dto.parentId,
            content = dto.content,
            mediaKey = dto.mediaUrl
        )
        return postRepo.saveAndRefresh(post)
    }

    private fun savePostStats(post: Post, user: UserResponseDto) {
        val stats = PostStats(
            postId = post.id!!,
            userId = post.userId,
            username = user.username,
            content = post.content,
            mediaKey = post.mediaKey
        )
        postStatsRepo.save(stats)
    }

    @Transactional(readOnly = true)
    override fun getById(id: Long): PostResponseDto {
        val post = postRepo.findByIdAndDeletedFalse(id)
            ?: throw PostNotFoundException()
        return toResponse(post)
    }

    override fun listPosts(archived: Boolean, pageable: Pageable): Page<PostResponseDto> {
        val posts = postRepo.listPosts(archived, pageable)
        return mapPage(posts)
    }

    override fun getByUserId(userId: Long, pageable: Pageable): Page<PostResponseDto> {
        return mapPage(postRepo.findByUserIdAndDeletedFalse(userId, pageable))
    }

    override fun getReplies(parentId: Long, pageable: Pageable): Page<PostResponseDto> {
        postRepo.findByIdAndDeletedFalse(parentId)
            ?: throw ParentPostNotFoundException(" Parent post not found with id: $parentId")
        return mapPage(postRepo.findByParentIdAndDeletedFalse(parentId, pageable))
    }

    @Transactional
    override fun update(id: Long, dto: PostUpdateDto, currentUserId: Long): PostResponseDto {
        val post = getOwnedPost(id, currentUserId)

        post.content = dto.content
        post.mediaKey = dto.mediaUrl
        postRepo.save(post)

        postStatsRepo.findByPostId(id)?.apply {
            content = dto.content
            mediaKey = dto.mediaUrl
            postStatsRepo.save(this)
        }

        return toResponse(post)
    }

    @Transactional
    override fun delete(id: Long, currentUserId: Long) {
        val post = getOwnedPost(id, currentUserId)
        postRepo.trash(post.id!!)
        postStatsRepo.findByPostId(id)?.let {
            it.deleted = true
            postStatsRepo.save(it)
        }
    }

    @Transactional
    override fun toggleLike(postId: Long, userId: Long): LikeResponseDto {
        postRepo.findByIdAndDeletedFalse(postId) ?: throw PostNotFoundException()

        val liked = try {
            reactionFeignClient.toggleLike(userId, ReactionTargetType.POST, postId)
        } catch (e: FeignException) {
            throw ServiceUnavailableException(ErrorCodes.REACTION_SERVICE_UNAVAILABLE, e.message ?: "Reaction error")
        }

        val stats = postStatsRepo.findByPostIdAndDeletedFalse(postId)

        stats.likeCount =
            if (liked) stats.likeCount + 1 else maxOf(0, stats.likeCount - 1)

        postStatsRepo.save(stats)

        return LikeResponseDto(liked, stats.likeCount)
    }

    override fun getPostStats(postId: Long): PostStatsResponseDto {
        val stats = postStatsRepo.findByPostIdAndDeletedFalse(postId)

        return PostStatsResponseDto(
            postId = postId,
            likeCount = stats.likeCount,
            commentCount = stats.commentCount,
            replyCount = stats.commentCount
        )
    }

    override fun postCountByUserId(userId: Long) = postRepo.countByUserIdAndDeletedFalse(userId)

    private fun toResponse(post: Post): PostResponseDto {
        val stats = postStatsRepo.findByPostIdAndDeletedFalse(post.id!!)
        val commentCount = fetchCommentCount(post.id!!)

        return PostResponseDto(
            id = post.id!!,
            userId = post.userId,
            username = stats.username,
            parentId = post.parentId,
            content = stats.content,
            mediaUrl = stats.mediaKey,
            createdAt = post.createdAt,
            hasSubPosts = stats.hasSubPosts,
            commentCount = commentCount,
            likeCount = stats.likeCount,
            archived = post.archived
        )
    }

    private fun mapPage(page: Page<Post>): Page<PostResponseDto> =
        page.map { toResponse(it) }

    private fun getOwnedPost(id: Long, userId: Long): Post {
        val post = postRepo.findByIdAndDeletedFalse(id)
            ?: throw PostNotFoundException()
        if (post.userId != userId) {
            throw AccessDeniedException("Not post owner")
        }
        return post
    }

    private fun updateHasSubPosts(parentId: Long) {
        postStatsRepo.findByPostId(parentId)?.apply {
            hasSubPosts = true
            postStatsRepo.save(this)
        }
    }

    private fun fetchUser(userId: Long): UserResponseDto =
        try {
            userFeignClient.getUserById(userId)
        } catch (e: FeignException) {
            throw ServiceUnavailableException(ErrorCodes.USER_SERVICE_UNAVAILABLE, e.message ?: "User service error")
        }

    private fun fetchCommentCount(postId: Long): Long =
        try {
            commentFeignClient.getCommentCount(postId)
        } catch (e: FeignException) {
            0L
        }
}
