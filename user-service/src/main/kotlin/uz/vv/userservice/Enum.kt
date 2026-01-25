package uz.vv.userservice

enum class Status {
    ACTIVE,
    INACTIVE,
}

enum class ErrorCodes(val code: Int, val msg: String) {
    USER_NOT_FOUND(100, "User not found in our database"),
    USERNAME_ALREADY_EXISTS(101, "Username is already taken"),
    VALIDATION_EXCEPTION(111, "Validation exception"),
    UNAUTHORIZED(200, "Authentication is required to access this resource"),
    ACCESS_DENIED(201, "You do not have permission to perform this action"),

    INTERNAL_SERVER_ERROR( 555, "Internal server error occurred"),
}