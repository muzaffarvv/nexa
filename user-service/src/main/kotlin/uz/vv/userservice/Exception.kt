package uz.vv.userservice

import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import jakarta.servlet.http.HttpServletResponse

sealed class BaseException(
    val errorCode: ErrorCodes,
    override val message: String = errorCode.name
) : RuntimeException(message)

class UserNotFoundException(
    errorCode: ErrorCodes = ErrorCodes.USER_NOT_FOUND
) : BaseException(errorCode)

class ValidationException(
    errorCode: ErrorCodes = ErrorCodes.VALIDATION_EXCEPTION,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.name)

class AlreadyExistsException(
    errorCode: ErrorCodes = ErrorCodes.USERNAME_ALREADY_EXISTS,
    message: String? = null
) : BaseException(errorCode, message ?: errorCode.name)

class AccessDeniedException(
    errorCode: ErrorCodes = ErrorCodes.ACCESS_DENIED
) : BaseException(errorCode)


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
            ErrorCodes.USER_NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorCodes.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
            ErrorCodes.ACCESS_DENIED -> HttpStatus.FORBIDDEN
            ErrorCodes.USERNAME_ALREADY_EXISTS -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        response.status = status.value()

        val requestId = request.getHeader("J-Request-Id")

        return ApiResponse(
            success = false,
            error = ErrorResponse(
                code = error.code,
                message = if (ex.message == error.name) error.msg else ex.message,
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
        val message = firstError?.let { "${it.field}: ${it.defaultMessage}" } ?: ErrorCodes.VALIDATION_EXCEPTION.msg
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