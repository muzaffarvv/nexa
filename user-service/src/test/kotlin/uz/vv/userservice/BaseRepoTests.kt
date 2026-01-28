package uz.vv.userservice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(BaseRepoImpl::class)
class BaseRepoTests {

    @Autowired
    private lateinit var userRepo: UserRepo

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Test
    fun `trash should soft delete entity`() {
        // Create and save a user
        val user = User(
            username = "testuser",
            mediaKey = "default.png",
            isPrivate = false
        )
        val saved = userRepo.save(user)
        entityManager.flush()
        entityManager.clear()

        val userId = saved.id!!

        // Soft delete the user
        userRepo.trash(userId)
        entityManager.flush()
        entityManager.clear()

        // Verify entity is marked as deleted
        val deleted = userRepo.findById(userId).get()
        assertTrue(deleted.deleted, "User should be marked as deleted")
    }

    @Test
    fun `findByIdAndDeletedFalse should not return deleted entities`() {
        // Create and save a user
        val user = User(
            username = "testuser2",
            mediaKey = "default.png",
            isPrivate = false
        )
        val saved = userRepo.save(user)
        entityManager.flush()
        entityManager.clear()

        val userId = saved.id!!

        // Verify we can find it before deletion
        val found = userRepo.findByIdAndDeletedFalse(userId)
        assertNotNull(found, "Should find active user")

        // Soft delete the user
        userRepo.trash(userId)
        entityManager.flush()
        entityManager.clear()

        // Verify we cannot find it after deletion
        val notFound = userRepo.findByIdAndDeletedFalse(userId)
        assertNull(notFound, "Should not find deleted user")
    }

    @Test
    fun `findAllNotDeleted should not return deleted entities`() {
        // Create and save two users
        val user1 = User(
            username = "activeuser",
            profileImageUrl = "default.png",
            isPrivate = false
        )
        val user2 = User(
            username = "deleteduser",
            profileImageUrl = "default.png",
            isPrivate = false
        )
        
        val saved1 = userRepo.save(user1)
        val saved2 = userRepo.save(user2)
        entityManager.flush()
        entityManager.clear()

        // Soft delete user2
        userRepo.trash(saved2.id!!)
        entityManager.flush()
        entityManager.clear()

        // Verify findAllNotDeleted only returns active users
        val activeUsers = userRepo.findAllNotDeleted()
        
        assertTrue(activeUsers.any { it.id == saved1.id }, "Should include active user")
        assertFalse(activeUsers.any { it.id == saved2.id }, "Should not include deleted user")
    }

    @Test
    fun `saveAndRefresh should return entity with updated fields`() {
        // Create and save a user
        val user = User(
            username = "refreshuser",
            profileImageUrl = "default.png",
            isPrivate = false
        )
        
        val saved = userRepo.saveAndRefresh(user)

        // Verify entity has generated ID and audit fields
        assertNotNull(saved.id, "ID should be generated")
        assertNotNull(saved.createdAt, "CreatedAt should be set")
        assertNotNull(saved.updatedAt, "UpdatedAt should be set")
    }
}
