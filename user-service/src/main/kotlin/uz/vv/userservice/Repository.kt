package uz.vv.userservice

import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
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
interface UserRepo : BaseRepo<User> {
    fun findByUsernameAndDeletedFalse(username: String): User?
    fun existsByUsernameAndDeletedFalse(username: String): Boolean

    @Modifying
    @Query("""
        update User u
        set u.mediaKey = :mediaKey,
            u.updatedAt = CURRENT_TIMESTAMP
        where u.id = :userId
          and u.deleted = false
          and u.status = 'ACTIVE'
    """)
    fun updateMediaKeyByUserId(
        @Param("userId") userId: Long,
        @Param("mediaKey") mediaKey: String
    ): Int

}

@Repository
interface UserAuthRepo : JpaRepository<UserAuth, Long> {
    fun findByUserId(userId: Long): UserAuth?

}