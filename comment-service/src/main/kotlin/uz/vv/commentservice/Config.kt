package uz.vv.commentservice

import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus

@Configuration
class FeignConfig {

    @Bean
    fun feignErrorDecoder(): ErrorDecoder {
        return FeignErrorDecoder()
    }
}

class FeignErrorDecoder : ErrorDecoder {
    private val defaultDecoder = ErrorDecoder.Default()

    override fun decode(methodKey: String, response: Response): Exception {
        val status = HttpStatus.valueOf(response.status())
        
        return when (status) {
            HttpStatus.NOT_FOUND -> {
                when {
                    methodKey.contains("UserFeignClient") -> 
                        InternalServerException(ErrorCodes.INTERNAL_SERVER_ERROR, "User not found")
                    else -> 
                        CommentNotFoundException()
                }
            }
            HttpStatus.CONFLICT -> 
                ValidationException(message = "Conflict occurred")
            HttpStatus.BAD_REQUEST -> 
                ValidationException(message = "Bad request")
            HttpStatus.FORBIDDEN -> 
                CommentAccessDeniedException()
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.INTERNAL_SERVER_ERROR -> 
                InternalServerException(ErrorCodes.INTERNAL_SERVER_ERROR, "Service unavailable")
            else -> defaultDecoder.decode(methodKey, response)
        }
    }
}
