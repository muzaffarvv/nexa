package uz.vv.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

data class ErrorResponse(
    val code: Int,
    val message: String,
    val errorName: String,
    val requestId: String? = null,
    val path: String? = null
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null
)

@Component
@Order(-2)
class GlobalErrorWebExceptionHandler(
    private val objectMapper: ObjectMapper
) : ErrorWebExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler::class.java)

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val request = exchange.request
        val response = exchange.response
        
        val requestId = request.headers.getFirst(REQUEST_ID_HEADER)
        val path = request.uri.path

        val (status, errorResponse) = when (ex) {
            is ResponseStatusException -> {
                val httpStatus = HttpStatus.valueOf(ex.statusCode.value())
                httpStatus to createErrorResponse(
                    code = ex.statusCode.value(),
                    message = ex.reason ?: httpStatus.reasonPhrase,
                    errorName = httpStatus.name,
                    requestId = requestId,
                    path = path
                )
            }
            is IllegalArgumentException -> {
                HttpStatus.BAD_REQUEST to createErrorResponse(
                    code = HttpStatus.BAD_REQUEST.value(),
                    message = ex.message ?: "Invalid request",
                    errorName = "BAD_REQUEST",
                    requestId = requestId,
                    path = path
                )
            }
            else -> {
                log.error("Unhandled exception in gateway", ex)
                HttpStatus.INTERNAL_SERVER_ERROR to createErrorResponse(
                    code = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    message = "Internal server error occurred",
                    errorName = "INTERNAL_SERVER_ERROR",
                    requestId = requestId,
                    path = path
                )
            }
        }

        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON

        val apiResponse = ApiResponse<Nothing>(
            success = false,
            error = errorResponse
        )

        val dataBuffer = response.bufferFactory().wrap(
            objectMapper.writeValueAsBytes(apiResponse)
        )

        return response.writeWith(Mono.just(dataBuffer))
    }

    private fun createErrorResponse(
        code: Int,
        message: String,
        errorName: String,
        requestId: String?,
        path: String?
    ): ErrorResponse {
        return ErrorResponse(
            code = code,
            message = message,
            errorName = errorName,
            requestId = requestId,
            path = path
        )
    }
}
