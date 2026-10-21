package co.onmind.hex4w.infrastructure.configuration;

import co.onmind.hex4w.transverse.resilience.OutboundGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the outbound limits are wired and that the configured values reach the
 * resilience policies.
 */
@SpringBootTest
@ActiveProfiles("test")
class ResilienceConfigurationIT {

    @Autowired
    @Qualifier("abcGuard")
    private OutboundGuard abcGuard;

    @Autowired
    @Qualifier("s3Guard")
    private OutboundGuard s3Guard;

    @Autowired
    @Qualifier("lambdaGuard")
    private OutboundGuard lambdaGuard;

    @Test
    @DisplayName("binds the configured limits of every downstream")
    void bindsConfiguredLimits() {
        assertEquals(20, abcGuard.bulkhead().getBulkheadConfig().getMaxConcurrentCalls());
        assertEquals(50, abcGuard.rateLimiter().getRateLimiterConfig().getLimitForPeriod());

        assertEquals(10, s3Guard.bulkhead().getBulkheadConfig().getMaxConcurrentCalls());
        assertEquals(20, s3Guard.rateLimiter().getRateLimiterConfig().getLimitForPeriod());

        assertEquals(10, lambdaGuard.bulkhead().getBulkheadConfig().getMaxConcurrentCalls());
        assertEquals(20, lambdaGuard.rateLimiter().getRateLimiterConfig().getLimitForPeriod());
    }

    @Test
    @DisplayName("rejects immediately instead of queueing the excess traffic")
    void rejectsWithoutWaiting() {
        assertEquals(0, abcGuard.bulkhead().getBulkheadConfig().getMaxWaitDuration().toMillis());
        assertEquals(0, abcGuard.rateLimiter().getRateLimiterConfig().getTimeoutDuration().toMillis());
    }

    @Test
    @DisplayName("keeps a circuit breaker for every downstream")
    void keepsCircuitBreakers() {
        assertNotNull(abcGuard.circuitBreaker());
        assertNotNull(s3Guard.circuitBreaker());
        assertNotNull(lambdaGuard.circuitBreaker());
        assertEquals("abc", abcGuard.circuitBreaker().getName());
        assertEquals("s3", s3Guard.circuitBreaker().getName());
        assertEquals("lambda", lambdaGuard.circuitBreaker().getName());
    }
}
