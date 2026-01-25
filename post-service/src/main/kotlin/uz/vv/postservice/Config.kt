package uz.vv.postservice

import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.http.HttpStatus

@Configuration
@EnableJpaAuditing
@EnableFeignClients
class AppConfig

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
                        ServiceUnavailableException(ErrorCodes.USER_SERVICE_UNAVAILABLE, "User not found")
                    else -> 
                        PostNotFoundException("Resource not found")
                }
            }
            HttpStatus.CONFLICT -> 
                ValidationException("Conflict occurred")
            HttpStatus.BAD_REQUEST -> 
                ValidationException("Bad request")
            HttpStatus.FORBIDDEN -> 
                AccessDeniedException("Access denied")
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.INTERNAL_SERVER_ERROR -> 
                ServiceUnavailableException(ErrorCodes.USER_SERVICE_UNAVAILABLE, "Service unavailable")
            else -> defaultDecoder.decode(methodKey, response)
        }
    }
}
