package uz.vv.userservice

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: UserCreateDto): Boolean {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginDto): UserResponseDto {
        return authService.login(request)
    }
}

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): UserResponseDto {
        return userService.getById(id)
    }

    @GetMapping("/all")
    fun getAll(pageable: Pageable): Page<UserResponseDto> {
        return userService.getAll(pageable)
    }

    @PutMapping("/{id}/update-info")
    fun updateProfile(
        @PathVariable id: Long,
        @RequestBody dto: UserInfoUpdateDTO
    ): UserResponseDto {
        return userService.updateProfile(id, dto)
    }

    @PutMapping("/{id}/update-security")
    fun updateSecurity(
        @PathVariable id: Long,
        @RequestBody dto: UserSecurityUpdateDTO
    ): UserResponseDto {
        return userService.updateSecurity(id, dto)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        userService.delete(id)
    }
}