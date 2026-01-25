package uz.vv.userrelationservice

enum class Status{
    ACTIVE,
    INACTIVE,
}

enum class ErrorCodes(val code: Int, val msg: String) {

    USER_NOT_FOUND(300, "User not found"),
    ACTION_NOT_ALLOWED(301, "Action not allowed"),
    ALREADY_FOLLOWING(302, "Already following"),
    FOLLOW_REQUEST_EXISTS(303, "Follow request already exists"),
    FOLLOW_RELATION_NOT_FOUND(304, "Follow relation not found"),
    BLOCK_RELATION_NOT_FOUND(305, "Block relation not found"),
    SELF_ACTION_NOT_ALLOWED(306, "You cannot perform this action on yourself"),

    VALIDATION_ERROR(400, "Data entered incorrectly"),
    INTERNAL_ERROR(500, "Internal server error")
}
