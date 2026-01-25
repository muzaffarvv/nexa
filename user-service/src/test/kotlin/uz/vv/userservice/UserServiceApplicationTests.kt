package uz.vv.userservice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest
@Suppress("DEPRECATION")
class UserServiceApplicationTests {

	@Autowired
	private lateinit var userService: UserService

	@MockBean
	private lateinit var userRepo: UserRepo

	@MockBean
	private lateinit var userAuthRepo: UserAuthRepo

	@MockBean
	private lateinit var passwordEncoder: PasswordEncoder

	@Test
	fun contextLoads() {
	}

	@Test
	fun `should throw exception when password is null`() {
		`when`(userRepo.existsByUsernameAndDeletedFalse("testuser")).thenReturn(false)

		val dto = UserCreateDto(
			username = "testuser",
			password = null,
			confirmPassword = null,
			isPrivate = false
		)

		val exception = assertThrows<ValidationException> {
			userService.create(dto)
		}

		assertTrue(exception.message.contains("Password") || exception.message.contains("required"))
	}

	@Test
	fun `should throw exception when passwords don't match`() {
		`when`(userRepo.existsByUsernameAndDeletedFalse("testuser")).thenReturn(false)

		val dto = UserCreateDto(
			username = "testuser",
			password = "password123",
			confirmPassword = "different",
			isPrivate = false
		)

		val exception = assertThrows<ValidationException> {
			userService.create(dto)
		}

		assertTrue(exception.message.contains("match"))
	}

	@Test
	fun `should throw exception when password is too short`() {
		`when`(userRepo.existsByUsernameAndDeletedFalse("testuser")).thenReturn(false)

		val dto = UserCreateDto(
			username = "testuser",
			password = "123",
			confirmPassword = "123",
			isPrivate = false
		)

		val exception = assertThrows<ValidationException> {
			userService.create(dto)
		}

		assertTrue(exception.message.contains("6 characters") || exception.message.contains("least"))
	}

	@Test
	fun `should throw exception when password is empty`() {
		`when`(userRepo.existsByUsernameAndDeletedFalse("testuser")).thenReturn(false)

		val dto = UserCreateDto(
			username = "testuser",
			password = "   ",
			confirmPassword = "   ",
			isPrivate = false
		)

		val exception = assertThrows<ValidationException> {
			userService.create(dto)
		}

		assertTrue(exception.message.contains("empty") || exception.message.contains("required"))
	}

	@Test
	fun `should throw exception when username already exists`() {
		`when`(userRepo.existsByUsernameAndDeletedFalse("existing")).thenReturn(true)

		val dto = UserCreateDto(
			username = "existing",
			password = "password123",
			confirmPassword = "password123",
			isPrivate = false
		)

		assertThrows<AlreadyExistsException> {
			userService.create(dto)
		}
	}

}
