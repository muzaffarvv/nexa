package uz.vv.userservice

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface BaseService<CreateDto, UpdateDto, UpdateSecurityDto, ResponseDto> {
    fun create(dto: CreateDto): ResponseDto
    fun updateProfile(id: Long, dto: UpdateDto): ResponseDto
    fun updateSecurity(id: Long, dto: UpdateSecurityDto): ResponseDto
    fun getById(id: Long): ResponseDto
    fun getAllList(): List<ResponseDto>
    fun getAll(pageable: Pageable): Page<ResponseDto>
    fun delete(id: Long)
}

abstract class BaseServiceImpl<
        E : BaseEntity,
        CreateDto,
        UpdateDto,
        UpdateSecurityDto,
        ResponseDto,
        Mapper : BaseMapper<E, ResponseDto>,
        Repo : BaseRepo<E>
        >(
    protected val repository: Repo,
    protected val mapper: Mapper
) : BaseService<CreateDto, UpdateDto, UpdateSecurityDto, ResponseDto> {

    protected abstract fun toEntity(dto: CreateDto): E
    protected abstract fun updateEntity(dto: UpdateDto, entity: E): E
    protected abstract fun updateSecurityEntity(dto: UpdateSecurityDto, entity: E): E
    protected abstract fun getUserById(id: Long): E

    @Transactional
    override fun create(dto: CreateDto): ResponseDto {
        val entity = toEntity(dto)
        val saved = repository.saveAndRefresh(entity)
        return mapper.toDto(saved)
    }

    @Transactional
    override fun updateProfile(id: Long, dto: UpdateDto): ResponseDto {
        val entity = getUserById(id)
        val updated = updateEntity(dto, entity)
        return mapper.toDto(repository.saveAndRefresh(updated))
    }

    @Transactional
    override fun updateSecurity(id: Long, dto: UpdateSecurityDto): ResponseDto {
        val entity = getUserById(id)
        val updated = updateSecurityEntity(dto, entity)
        return mapper.toDto(repository.saveAndRefresh(updated))
    }

    @Transactional(readOnly = true)
    override fun getById(id: Long): ResponseDto =
        mapper.toDto(getUserById(id))

    @Transactional(readOnly = true)
    override fun getAllList(): List<ResponseDto> =
        repository.findAllNotDeleted().map { mapper.toDto(it) }

    @Transactional(readOnly = true)
    override fun getAll(pageable: Pageable): Page<ResponseDto> =
        repository.findAllNotDeleted(pageable).map { mapper.toDto(it) }

    @Transactional
    override fun delete(id: Long) {
        repository.trash(id)
            ?: throw UserNotFoundException(ErrorCodes.USER_NOT_FOUND)
    }
}

@Service
class UserService(
    repository: UserRepo,
    private val userAuthRepo: UserAuthRepo,
    private val passwordEncoder: PasswordEncoder,
    private val mediaFeignClient: MediaFeignClient,
    mapper: UserMapper
) : BaseServiceImpl<
        User,
        UserCreateDto,
        UserInfoUpdateDTO,
        UserSecurityUpdateDTO,
        UserResponseDto,
        UserMapper,
        UserRepo
        >(repository, mapper) {

    override fun toEntity(dto: UserCreateDto): User {
        checkUsername(dto.username)
        val validatedPassword = dto.password
        validatePassword(validatedPassword, dto.confirmPassword)

        val user = repository.save(buildUser(dto))
        saveUserAuth(user, validatedPassword)

        return user
    }

    fun checkUsername(username: String) {
        if (repository.existsByUsernameAndDeletedFalse(username)) {
            throw AlreadyExistsException(
                errorCode = ErrorCodes.USERNAME_ALREADY_EXISTS,
                message = "Username '$username' is already taken"
            )
        }
    }

    private fun buildUser(dto: UserCreateDto) =
        User(
            fullName = dto.fullName,
            username = dto.username,
            phoneNumber = dto.phoneNumber,
            bio = dto.bio,
            mediaKey = dto.mediaKey ?: "default.png",
            age = dto.age,
            isPrivate = dto.isPrivate
        )

    private fun saveUserAuth(user: User, rawPassword: String) {
        val userId = user.id ?: throw IllegalStateException("User ID must be set before creating auth")
        val encodedPassword = passwordEncoder.encode(rawPassword)
        
        val auth = userAuthRepo.findByUserId(userId)
            ?: UserAuth(user = user, passwordHash = encodedPassword)
        
        auth.passwordHash = encodedPassword
        userAuthRepo.save(auth)
    }

    private fun validatePassword(password: String?, confirm: String?) {
        when {
            password.isNullOrBlank() -> throw ValidationException(
                ErrorCodes.VALIDATION_EXCEPTION,
                "Password cannot be empty"
            )
            password != confirm -> throw ValidationException(
                ErrorCodes.VALIDATION_EXCEPTION,
                "Passwords don't match"
            )
            password.length < 6 -> throw ValidationException(
                ErrorCodes.VALIDATION_EXCEPTION,
                "Password must be at least 6 characters long"
            )
        }
    }



    override fun updateEntity(dto: UserInfoUpdateDTO, entity: User): User {
        dto.fullName?.let { entity.fullName = it }
        dto.bio?.let { entity.bio = it }
        dto.mediaKey?.let { entity.mediaKey = it }
        dto.age?.let { entity.age = it }
        return entity
    }

    override fun updateSecurityEntity(dto: UserSecurityUpdateDTO, entity: User): User {

        dto.username?.let {
            if (repository.existsByUsernameAndDeletedFalse(it) && it != entity.username) {
                throw AlreadyExistsException(ErrorCodes.USERNAME_ALREADY_EXISTS)
            }
            entity.username = it
        }

        dto.isPrivate?.let { entity.isPrivate = it }

        dto.newPassword?.let {
            validatePassword(it, dto.confirmPassword)
            saveUserAuth(entity, it)
        }

        return entity
    }


    override fun getUserById(id: Long): User =
        repository.findByIdAndDeletedFalse(id)
            ?: throw UserNotFoundException(ErrorCodes.USER_NOT_FOUND)

    @Transactional(readOnly = true)
    override fun getById(id: Long): UserResponseDto {
        val user = getUserById(id)
        val profileImageUrl = fetchProfileImageUrl(user.id!!, user.mediaKey ?: "")
        user.mediaKey = profileImageUrl
        return mapper.toDto(user)
    }

    @Transactional(readOnly = true)
    override fun getAll(pageable: Pageable): Page<UserResponseDto> {
        return repository.findAllNotDeleted(pageable).map { user ->
            val profileImageUrl = fetchProfileImageUrl(user.id!!, user.mediaKey ?: "")
            user.mediaKey = profileImageUrl
            mapper.toDto(user)
        }
    }

    @Transactional(readOnly = true)
    override fun getAllList(): List<UserResponseDto> {
        return repository.findAllNotDeleted().map { user ->
            val profileImageUrl = fetchProfileImageUrl(user.id!!, user.mediaKey ?: "")
            user.mediaKey = profileImageUrl
            mapper.toDto(user)
        }
    }

    internal fun findByUsername(username: String): User? =
        repository.findByUsernameAndDeletedFalse(username)

    /**
     * Fetches profile image URL from media-service for the given user.
     * Falls back to the user's stored profileImageUrl if media-service is unavailable or returns empty list.
     */
    internal fun fetchProfileImageUrl(userId: Long, fallbackUrl: String): String {
        return try {
            val mediaFiles = mediaFeignClient.getMediaByOwner("USER", userId)
            mediaFiles.firstOrNull()?.url ?: fallbackUrl
        } catch (e: Exception) {
            // If media-service is unavailable or throws error, use fallback
            fallbackUrl
        }
    }
}

//    override fun delete(id: Long) {
//        super.delete(id)
//        // TODO user-delete => user's posts, profilePic,
//    }

interface AuthService {
    fun register(request: UserCreateDto): Boolean
    fun login(request: LoginDto): UserResponseDto
}

@Service
class AuthServiceImpl(
    private val userService: UserService,
    private val userAuthRepo: UserAuthRepo,
    private val passwordEncoder: PasswordEncoder,
    private val mapper: UserMapper
) : AuthService {

    @Transactional
    override fun register(request: UserCreateDto): Boolean {
        userService.create(request)
        return true
    }

    @Transactional(readOnly = true)
    override fun login(request: LoginDto): UserResponseDto {

        val user = userService.findByUsername(request.username)
            ?: throw ValidationException(
                ErrorCodes.VALIDATION_EXCEPTION,
                "Invalid username or password"
            )

        val userId = user.id ?: throw ValidationException(
            ErrorCodes.VALIDATION_EXCEPTION,
            "Invalid user state"
        )

        val auth = userAuthRepo.findByUserId(userId)
            ?: throw ValidationException(
                ErrorCodes.VALIDATION_EXCEPTION,
                "Invalid username or password"
            )

        if (!passwordEncoder.matches(request.password, auth.passwordHash)) {
            throw ValidationException(
                ErrorCodes.VALIDATION_EXCEPTION,
                "Invalid username or password"
            )
        }

        return mapper.toDto(user)
    }
}