package uz.vv.userrelationservice

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

sealed class BaseException(
    val errorCode: ErrorCodes,
    message: String? = null
) : RuntimeException(message ?: errorCode.msg)

class UserNotFoundException(
    message: String? = null,
    errorCode: ErrorCodes = ErrorCodes.USER_NOT_FOUND
) : BaseException(errorCode, message)

class ActionNotAllowedException(
    message: String? = null,
    errorCode: ErrorCodes = ErrorCodes.ACTION_NOT_ALLOWED
) : BaseException(errorCode, message)

class AlreadyFollowingException(
    message: String? = null,
    errorCode: ErrorCodes = ErrorCodes.ALREADY_FOLLOWING
) : BaseException(errorCode, message)

class FollowRequestExistsException(
    message: String? = null,
    errorCode: ErrorCodes = ErrorCodes.FOLLOW_REQUEST_EXISTS
) : BaseException(errorCode, message)

class FollowRelationNotFoundException(
    message: String? = null,
    errorCode: ErrorCodes = ErrorCodes.FOLLOW_RELATION_NOT_FOUND
) : BaseException(errorCode, message)

class BlockRelationNotFoundException(
    message: String? = null,
    errorCode: ErrorCodes = ErrorCodes.BLOCK_RELATION_NOT_FOUND
) : BaseException(errorCode, message)

class SelfActionNotAllowedException(
    message: String? = null,
    errorCode: ErrorCodes = ErrorCodes.SELF_ACTION_NOT_ALLOWED
) : BaseException(errorCode, message)



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
)

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
            ErrorCodes.USER_NOT_FOUND,
            ErrorCodes.FOLLOW_RELATION_NOT_FOUND,
            ErrorCodes.BLOCK_RELATION_NOT_FOUND -> HttpStatus.NOT_FOUND

            ErrorCodes.ALREADY_FOLLOWING,
            ErrorCodes.FOLLOW_REQUEST_EXISTS,
            ErrorCodes.SELF_ACTION_NOT_ALLOWED -> HttpStatus.BAD_REQUEST

            ErrorCodes.ACTION_NOT_ALLOWED -> HttpStatus.FORBIDDEN

            else -> HttpStatus.BAD_REQUEST
        }

        response.status = status.value()

        val requestId = request.getHeader("J-Request-Id")

        return ApiResponse(
            success = false,
            error = ErrorResponse(
                code = error.code,
                message = ex.message ?: error.msg,
                errorName = error.name,
                requestId = requestId
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException, 
        response: HttpServletResponse,
        request: jakarta.servlet.http.HttpServletRequest
    ): ApiResponse<Nothing> {
        response.status = HttpStatus.BAD_REQUEST.value()

        val firstError = ex.bindingResult.fieldErrors.firstOrNull()
        val message = firstError?.let { "${it.field}: ${it.defaultMessage}" } ?: ErrorCodes.VALIDATION_ERROR.msg
        val requestId = request.getHeader("J-Request-Id")

        return ApiResponse(
            success = false,
            error = ErrorResponse(
                code = ErrorCodes.VALIDATION_ERROR.code,
                message = message,
                errorName = "VALIDATION_ERROR",
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
                code = ErrorCodes.INTERNAL_ERROR.code,
                message = ex.message ?: ErrorCodes.INTERNAL_ERROR.msg,
                errorName = "INTERNAL_SERVER_ERROR",
                requestId = requestId
            )
        )
    }
}

