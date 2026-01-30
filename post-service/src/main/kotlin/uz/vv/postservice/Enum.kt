package uz.vv.postservice

enum class Status {
    ACTIVE,
    INACTIVE
}

enum class ErrorCodes(val code: Int, val msg: String) {
    POST_NOT_FOUND(300, "Post not found"),
    PARENT_POST_NOT_FOUND(302, "Parent post not found"),
    VALIDATION_EXCEPTION(111, "Validation exception"),
    USER_SERVICE_UNAVAILABLE(400, "User service unavailable"),
    REACTION_SERVICE_UNAVAILABLE(401, "Reaction service unavailable"),
    INTERNAL_SERVER_ERROR(555, "Internal server error"),
    ACCESS_DENIED( 557, "Access denied"),
    FILE_NOT_FOUND(558, "File not found"),
}