package uz.vv.reactionservice

enum class ReactionTargetType {
    POST, COMMENT
}

enum class Status {
    ACTIVE,
    INACTIVE
}

enum class ErrorCodes(val code: Int, val msg: String) {
    // Reaction specific errors (500s)
    REACTION_NOT_FOUND(500, "Reaction not found"),
    REACTION_ALREADY_EXISTS(501, "You have already reacted to this content"),
    INVALID_TARGET_TYPE(502, "Invalid reaction target type"),
    
    // Resource errors (503-509)
    POST_NOT_FOUND(503, "Post not found"),
    COMMENT_NOT_FOUND(504, "Comment not found"),
    
    // Validation errors (111)
    VALIDATION_EXCEPTION(111, "Validation error occurred"),
    
    // System errors (555)
    INTERNAL_SERVER_ERROR(555, "Internal server error occurred")
}