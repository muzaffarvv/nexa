package uz.vv.postservice

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

sealed class BaseException(
    val errorCode: ErrorCodes,
    override val message: String? = null
) : RuntimeException(message ?: errorCode.msg)

class PostNotFoundException(message: String? = null) :
    BaseException(ErrorCodes.POST_NOT_FOUND, message)

class ParentPostNotFoundException(message: String? = null) :
    BaseException(ErrorCodes.PARENT_POST_NOT_FOUND, message)

class ServiceUnavailableException(errorCode: ErrorCodes, message: String? = null) :
    BaseException(errorCode, message)

class AccessDeniedException(message: String? = null) :
    BaseException(ErrorCodes.ACCESS_DENIED, message)

class ValidationException(message: String? = null) :
    BaseException(ErrorCodes.VALIDATION_EXCEPTION, message)

class InternalServerException(message: String? = null) :
    BaseException(ErrorCodes.INTERNAL_SERVER_ERROR, message)


data class ErrorResponse(
    val code: Int,
    val message: String?,
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
            ErrorCodes.POST_NOT_FOUND,
            ErrorCodes.PARENT_POST_NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorCodes.ACCESS_DENIED -> HttpStatus.FORBIDDEN
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