package uz.vv.commentservice

enum class Status {
    ACTIVE,
    INACTIVE,
}

enum class ErrorCodes(val code: Int, val msg: String) {
    COMMENT_NOT_FOUND(300, "Comment not found"),
    COMMENT_NOT_OWNED(301, "You can only edit/delete your own comments"),
    PARENT_COMMENT_NOT_FOUND(302, "Parent comment for reply not found"),
    VALIDATION_EXCEPTION(111, "Validation error occurred"),
    INTERNAL_SERVER_ERROR(555, "Internal server error occurred")
}