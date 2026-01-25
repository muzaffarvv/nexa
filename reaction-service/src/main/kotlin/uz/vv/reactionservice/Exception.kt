package uz.vv.reactionservice


import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

sealed class BaseException(
    val errorCode: ErrorCodes,
    override val message: String = errorCode.name
) : RuntimeException(message)

class ReactionNotFoundException(
    errorCode: ErrorCodes = ErrorCodes.REACTION_NOT_FOUND,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.msg)

class ReactionAlreadyExistsException(
    errorCode: ErrorCodes = ErrorCodes.REACTION_ALREADY_EXISTS,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.msg)

class InvalidTargetTypeException(
    errorCode: ErrorCodes = ErrorCodes.INVALID_TARGET_TYPE,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.msg)

class PostNotFoundException(
    errorCode: ErrorCodes = ErrorCodes.POST_NOT_FOUND,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.msg)

class CommentNotFoundException(
    errorCode: ErrorCodes = ErrorCodes.COMMENT_NOT_FOUND,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.msg)

class ValidationException(
    errorCode: ErrorCodes = ErrorCodes.VALIDATION_EXCEPTION,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.msg)

class InternalServerException(
    errorCode: ErrorCodes = ErrorCodes.INTERNAL_SERVER_ERROR,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.msg)

data class ErrorResponse(
    val code: Int,
    val message: String,
    val errorName: String,
    val requestId: String? = null
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null
) {
    companion object {
        fun <T> ok(data: T?): ApiResponse<T> = ApiResponse(success = true, data = data)
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(
        ex: BaseException, 
        response: HttpServletResponse,
        request: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<Nothing> {
        val error = ex.errorCode

        val status = when (error) {
            ErrorCodes.REACTION_NOT_FOUND,
            ErrorCodes.POST_NOT_FOUND,
            ErrorCodes.COMMENT_NOT_FOUND -> HttpStatus.NOT_FOUND
            
            ErrorCodes.REACTION_ALREADY_EXISTS -> HttpStatus.CONFLICT
            
            ErrorCodes.INVALID_TARGET_TYPE,
            ErrorCodes.VALIDATION_EXCEPTION -> HttpStatus.BAD_REQUEST
            
            ErrorCodes.INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
            
            else -> HttpStatus.BAD_REQUEST
        }

        response.status = status.value()

        val requestId = request.getHeader("J-Request-Id")

        return ApiResponse(
            success = false,
            error = ErrorResponse(
                code = error.code,
                message = ex.message,
                errorName = error.name,
                requestId = requestId
            )
        )
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: org.springframework.web.bind.MethodArgumentNotValidException,
        response: HttpServletResponse,
        request: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<Nothing> {
        response.status = HttpStatus.BAD_REQUEST.value()

        val firstError = ex.bindingResult.fieldErrors.firstOrNull()
        val message = firstError?.let { "${it.field}: ${it.defaultMessage}" } 
            ?: ErrorCodes.VALIDATION_EXCEPTION.msg
        val requestId = request.getHeader("J-Request-Id")

        return ApiResponse(
            success = false,
            error = ErrorResponse(
                code = ErrorCodes.VALIDATION_EXCEPTION.code,
                message = message,
                errorName = ErrorCodes.VALIDATION_EXCEPTION.name,
                requestId = requestId
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception, 
        response: HttpServletResponse,
        request: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<Nothing> {
        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()

        val requestId = request.getHeader("J-Request-Id")

        return ApiResponse(
            success = false,
            error = ErrorResponse(
                code = ErrorCodes.INTERNAL_SERVER_ERROR.code,
                message = ex.message ?: ErrorCodes.INTERNAL_SERVER_ERROR.msg,
                errorName = ErrorCodes.INTERNAL_SERVER_ERROR.name,
                requestId = requestId
            )
        )
    }
}