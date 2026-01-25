package uz.vv.gateway

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "gateway")
data class RestrictedPathProperties(
    var restrictedPaths: List<String> = emptyList()
)

@Configuration
@EnableConfigurationProperties(RestrictedPathProperties::class)
class GatewayConfiguration