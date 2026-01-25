package uz.vv.userservice

import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@EnableJpaAuditing
@EnableFeignClients
class JpaConfig


@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

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
                    methodKey.contains("MediaFeignClient") -> 
                        ValidationException(ErrorCodes.VALIDATION_EXCEPTION, "Media not found")
                    else -> 
                        UserNotFoundException(ErrorCodes.USER_NOT_FOUND)
                }
            }
            HttpStatus.BAD_REQUEST -> 
                ValidationException(ErrorCodes.VALIDATION_EXCEPTION, "Bad request")
            HttpStatus.FORBIDDEN -> 
                AccessDeniedException(ErrorCodes.ACCESS_DENIED)
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.INTERNAL_SERVER_ERROR -> 
                ValidationException(ErrorCodes.VALIDATION_EXCEPTION, "Service unavailable")
            else -> defaultDecoder.decode(methodKey, response)
        }
    }
}