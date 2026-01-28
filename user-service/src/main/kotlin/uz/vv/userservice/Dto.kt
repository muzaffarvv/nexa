package uz.vv.userservice

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserCreateDto(

    @field:NotBlank
    @field:Size(max = 100)
    val fullName: String? = null,

    @field:NotBlank(message = "username is required")
    @field:Size(min = 3, max = 30, message = "username must be between 3 and 30 characters")
    @field:Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9._]*$",
        message = "Username must start with a letter and can only contain letters, numbers, dots, or underscores"
    )
    val username: String,

    @field:NotBlank
    @field:Size(max = 15)
    @field:Pattern(
        regexp = "^[0-9]{9,15}$",
        message = "Phone number must be digits only and between 9 to 15 digits long"
    )
    val phoneNumber: String? = null,

    @field:Size(max = 150)
    val bio: String? = null,

    val mediaKey: String? = null,

    @field:Min(13, message = "You must be at least 13 years old to register")
    val age: Int? = null,

    val isPrivate: Boolean = false,

    @field:NotBlank
    @field:Size(min = 6, max = 30)
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%!?&]).*$",
        message = "Password must contain uppercase, lowercase, digit and special character"
    )    val password: String, // create da kerak, update da optional

    @field:NotBlank
    val confirmPassword: String? = null
)

data class UserInfoUpdateDTO(

    @field:Size(max = 100)
    val fullName: String? = null,

    @field:Size(max = 150)
    val bio: String? = null,

    val mediaKey: String? = null,

    @field:Min(13)
    val age: Int? = null,

)

data class UserSecurityUpdateDTO(

    @field:Size(min = 3, max = 30)
    @field:Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9._]*$",
        message = "Username must start with a letter and contain only letters, numbers, dots, or underscores"
    )
    val username: String? = null,

    @field:Size(min = 6)
    @field:Pattern(
        regexp = "^(?=.*[0-9])(?=.*[@#$%!?&]).*$",
        message = "Password must contain at least one digit and one special character"
    )
    val newPassword: String? = null,

    val confirmPassword: String? = null,

    val isPrivate: Boolean? = null
)

data class LoginDto(
    @field:NotBlank(message = "username is required")
    val username: String,

    @field:NotBlank(message = "password is required")
    val password: String,
)

data class UserResponseDto(
    val id: Long,
    val fullName: String?,
    val username: String,
    val phoneNumber: String?,
    val bio: String?,
    val mediaKey: String,
    val age: Int?,
    val isPrivate: Boolean
)

