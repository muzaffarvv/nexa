package uz.vv.gateway

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.route.Route
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
import org.springframework.http.server.reactive.ServerHttpRequest
import java.util.UUID
import org.springframework.http.HttpStatus
import java.util.regex.Pattern

@Component
class LoggingGlobalFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(LoggingGlobalFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {

        if (exchange.attributes.putIfAbsent(this.javaClass.getIsRoutedKey(), true) != null)
            return chain.filter(exchange)

        val request = exchange.request
        val route = exchange.getAttribute<Route>(GATEWAY_ROUTE_ATTR)

        log.info("---- GATEWAY REQUEST ----")
        log.info("ID: ${request.id}")
        log.info("METHOD: ${request.method}")
        log.info("PATH: ${request.uri.path}")
        log.info("ROUTE: ${route?.id}")
        log.info("SERVICE: ${request.uri.path.extractServiceName()}")

        return chain.filter(exchange)
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE + 2 // Runs third
}

@Component
class RequestIdGlobalFilter : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {

        if (exchange.attributes.putIfAbsent(this.javaClass.getIsRoutedKey(), true) != null)
            return chain.filter(exchange)

        val requestId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        val request: ServerHttpRequest = exchange.request
            .mutate()
            .header(REQUEST_ID_HEADER, requestId)
            .header(START_TIME_HEADER, startTime.toString())
            .build()

        return chain.filter(exchange.mutate().request(request).build())
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE + 1 // Runs second
}

@Component
class RestrictedPathFilter(
    props: RestrictedPathProperties
) : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(RestrictedPathFilter::class.java)
    private val patterns = props.restrictedPaths.map { Pattern.compile(it) }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {

        if (exchange.attributes.putIfAbsent(this.javaClass.getIsRoutedKey(), true) != null)
            return chain.filter(exchange)

        val path = exchange.request.uri.path

        patterns.forEach {
            if (it.matcher(path).matches()) {
                log.warn("Access denied to restricted path: $path")
                exchange.response.statusCode = HttpStatus.FORBIDDEN
                return exchange.response.setComplete()
            }
        }

        return chain.filter(exchange)
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE // Runs first (security)
}