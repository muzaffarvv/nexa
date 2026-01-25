package uz.vv.mediaservice

enum class MediaOwnerType {
    POST,
    COMMENT,
    USER
}

enum class MediaType {
    IMAGE,
    VIDEO,
    GIF
}

enum class Status {
    ACTIVE,
    INACTIVE,
}

enum class ErrorCodes(val code: Int, val msg: String) {
    // Media specific errors (600s)
    FILE_UPLOAD_FAILED(600, "File upload failed"),
    FILE_NOT_FOUND(601, "File not found"),
    FILE_EMPTY(602, "File cannot be empty"),
    INVALID_FILE_TYPE(603, "Invalid file type"),
    FILE_TOO_LARGE(604, "File size exceeds maximum allowed"),
    MEDIA_NOT_FOUND(605, "Media not found"),
    
    // Validation errors (111)
    VALIDATION_EXCEPTION(111, "Validation error occurred"),
    
    // System errors (555)
    INTERNAL_SERVER_ERROR(555, "Internal server error occurred")
}

