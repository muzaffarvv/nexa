package uz.vv.gateway

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered

@SpringBootTest
class GatewayApplicationTests {

    @Autowired
    private lateinit var filters: List<GlobalFilter>

    @Test
    fun contextLoads() {
    }

    @Test
    fun `filters should have correct order`() {
        val orderedFilters = filters
            .filterIsInstance<Ordered>()
            .sortedBy { it.order }

        // Verify we have at least 3 filters
        assertTrue(orderedFilters.size >= 3, "Should have at least 3 ordered filters")

        // Find our custom filters
        val restrictedPathFilter = orderedFilters.find { it is RestrictedPathFilter }
        val requestIdFilter = orderedFilters.find { it is RequestIdGlobalFilter }
        val loggingFilter = orderedFilters.find { it is LoggingGlobalFilter }

        // Verify all filters are present
        assertNotNull(restrictedPathFilter, "RestrictedPathFilter should be present")
        assertNotNull(requestIdFilter, "RequestIdGlobalFilter should be present")
        assertNotNull(loggingFilter, "LoggingGlobalFilter should be present")

        // Verify order values
        assertEquals(Ordered.HIGHEST_PRECEDENCE, (restrictedPathFilter as Ordered).order, 
            "RestrictedPathFilter should have HIGHEST_PRECEDENCE")
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 1, (requestIdFilter as Ordered).order,
            "RequestIdGlobalFilter should run second")
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 2, (loggingFilter as Ordered).order,
            "LoggingGlobalFilter should run third")

        // Verify execution order
        val restrictedIndex = orderedFilters.indexOf(restrictedPathFilter)
        val requestIdIndex = orderedFilters.indexOf(requestIdFilter)
        val loggingIndex = orderedFilters.indexOf(loggingFilter)

        assertTrue(restrictedIndex < requestIdIndex, 
            "RestrictedPathFilter should execute before RequestIdGlobalFilter")
        assertTrue(requestIdIndex < loggingIndex,
            "RequestIdGlobalFilter should execute before LoggingGlobalFilter")
    }

}
