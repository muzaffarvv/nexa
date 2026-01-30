package uz.vv.postservice

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

@NoRepositoryBean
interface BaseRepo<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
    fun saveAndRefresh(entity: T): T
}

class BaseRepoImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepo<T> {

    private val notDeleted = Specification<T> { root, _, cb ->
        cb.equal(root.get<Boolean>("deleted"), false)
    }

    override fun findByIdAndDeletedFalse(id: Long): T? =
        findOne(notDeleted.and { root, _, cb -> cb.equal(root.get<Long>("id"), id) }).orElse(null)

    @Transactional
    override fun trash(id: Long): T? = findById(id).orElse(null)?.apply {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(notDeleted)

    override fun findAllNotDeleted(pageable: Pageable) = findAll(notDeleted, pageable)

    @Transactional
    override fun saveAndRefresh(entity: T): T {
        val saved = save(entity)
        entityManager.flush()
        entityManager.refresh(saved)
        return saved
    }
}

@Repository
interface PostRepo : BaseRepo<Post> {

    @Query(
        """
        SELECT COUNT(p) FROM Post p 
        WHERE p.userId = :userId 
        AND p.deleted = false
    """
    )
    fun countByUserIdAndDeletedFalse(@Param("userId") userId: Long): Int

    @Query(
        """
        SELECT p FROM Post p 
        WHERE p.userId = :userId 
        AND p.deleted = false 
        ORDER BY p.createdAt DESC
    """
    )
    fun findByUserIdAndDeletedFalse(userId: Long, pageable: Pageable): Page<Post>

    @Query(
        """
        SELECT p FROM Post p 
        WHERE p.parentId = :parentId 
        AND p.deleted = false 
        ORDER BY p.createdAt DESC
    """
    )
    fun findByParentIdAndDeletedFalse(parentId: Long, pageable: Pageable): Page<Post>

    @Query(
        """
        SELECT COUNT(p) > 0 FROM Post p 
        WHERE p.parentId = :postId 
        AND p.deleted = false
    """
    )
    fun hasReplies(postId: Long): Boolean

    @Query(
        """
        SELECT p FROM Post p 
        WHERE p.deleted = false 
        AND p.archived = false 
        ORDER BY p.createdAt DESC
    """
    )
    fun findActivePostsNotDeleted(pageable: Pageable): Page<Post>

    @Query(
        """
        SELECT p FROM Post p
        WHERE p.deleted = false
          AND (:archived = false OR p.archived = true)
        """
    )
    fun listPosts(
        @Param("archived") archived: Boolean,
        pageable: Pageable
    ): Page<Post>
}

@Repository
interface PostStatsRepo : BaseRepo<PostStats> {
    fun findByPostIdAndDeletedFalse(id: Long): PostStats

    fun findByPostId(postId: Long): PostStats?

    fun findByPostIdIn(postIds: List<Long>): List<PostStats>

    @Query(
        """
        SELECT ps FROM PostStats ps 
        WHERE ps.userId = :userId 
        AND ps.deleted = false 
        ORDER BY ps.createdAt DESC
    """
    )
    fun findByUserIdAndDeletedFalse(userId: Long, pageable: Pageable): Page<PostStats>
}