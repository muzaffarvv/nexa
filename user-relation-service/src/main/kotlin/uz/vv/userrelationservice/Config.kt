package uz.vv.userrelationservice

import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.http.HttpStatus

@Configuration
@EnableJpaAuditing
class JpaConfig

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
                        UserNotFoundException(message = "User not found")
                    else -> 
                        UserNotFoundException(message = "Resource not found")
                }
            }
            HttpStatus.CONFLICT -> 
                AlreadyFollowingException(message = "Conflict occurred")
            HttpStatus.BAD_REQUEST -> 
                ActionNotAllowedException(message = "Bad request")
            HttpStatus.FORBIDDEN -> 
                ActionNotAllowedException(message = "Access denied")
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.INTERNAL_SERVER_ERROR -> 
                UserNotFoundException(message = "Service unavailable")
            else -> defaultDecoder.decode(methodKey, response)
        }
    }
}
