package co.onmind.hex4w.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits applied to the outbound calls of each downstream.
 * <p>
 * Every value has a sensible default, so the {@code app.resilience} block of
 * {@code application.yml} is entirely optional — it only needs to be present when a
 * downstream needs different limits.
 */
@ConfigurationProperties(prefix = "app.resilience")
public class ResilienceProperties {

    private final Downstream abc = new Downstream(20, 50);
    private final Downstream s3 = new Downstream(10, 20);
    private final Downstream lambda = new Downstream(10, 20);

    public Downstream getAbc() {
        return abc;
    }

    public Downstream getS3() {
        return s3;
    }

    public Downstream getLambda() {
        return lambda;
    }

    /**
     * Limits of a single downstream.
     *
     * @param maxConcurrentCalls maximum number of calls in flight at the same time
     * @param rateLimitPerPeriod  maximum number of calls allowed per refresh period
     */
    public static class Downstream {

        private int maxConcurrentCalls;
        private long maxWaitMs;
        private int rateLimitPerPeriod;
        private long rateLimitRefreshMs;
        private long rateLimitTimeoutMs;

        public Downstream() {
        }

        public Downstream(int maxConcurrentCalls, int rateLimitPerPeriod) {
            this.maxConcurrentCalls = maxConcurrentCalls;
            this.maxWaitMs = 0;
            this.rateLimitPerPeriod = rateLimitPerPeriod;
            this.rateLimitRefreshMs = 1000;
            this.rateLimitTimeoutMs = 0;
        }

        public int getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public void setMaxConcurrentCalls(int maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
        }

        public long getMaxWaitMs() {
            return maxWaitMs;
        }

        public void setMaxWaitMs(long maxWaitMs) {
            this.maxWaitMs = maxWaitMs;
        }

        public int getRateLimitPerPeriod() {
            return rateLimitPerPeriod;
        }

        public void setRateLimitPerPeriod(int rateLimitPerPeriod) {
            this.rateLimitPerPeriod = rateLimitPerPeriod;
        }

        public long getRateLimitRefreshMs() {
            return rateLimitRefreshMs;
        }

        public void setRateLimitRefreshMs(long rateLimitRefreshMs) {
            this.rateLimitRefreshMs = rateLimitRefreshMs;
        }

        public long getRateLimitTimeoutMs() {
            return rateLimitTimeoutMs;
        }

        public void setRateLimitTimeoutMs(long rateLimitTimeoutMs) {
            this.rateLimitTimeoutMs = rateLimitTimeoutMs;
        }
    }
}
