package com.arbitrier.platform.messaging.outbox.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Outbox polling runtime.
 *
 * <p>All properties are prefixed with {@code arbitrier.messaging.outbox.polling}.
 *
 * <h2>Defaults</h2>
 * <table>
 *   <tr><th>Property</th><th>Default</th><th>Meaning</th></tr>
 *   <tr><td>{@code enabled}</td><td>{@code true}</td><td>Polling scheduler is active on startup</td></tr>
 *   <tr><td>{@code initial-delay-ms}</td><td>5 000</td><td>Delay before the first polling cycle fires</td></tr>
 *   <tr><td>{@code fixed-delay-ms}</td><td>10 000</td><td>Delay between end of one cycle and start of the next</td></tr>
 *   <tr><td>{@code batch-size}</td><td>100</td><td>Maximum messages claimed per polling cycle</td></tr>
 *   <tr><td>{@code worker-id}</td><td>{@code ""}</td><td>Worker identity recorded on each claim row; auto-generated from hostname if blank</td></tr>
 * </table>
 *
 * <p>Layer: platform/messaging/outbox/spring
 * <p>Module: platform
 */
@ConfigurationProperties(prefix = "arbitrier.messaging.outbox.polling")
public class OutboxPollingProperties {

    /** Whether the outbox polling scheduler is active. */
    private boolean enabled = true;

    /** Milliseconds between the end of one polling cycle and the start of the next. */
    private long fixedDelayMs = 10_000;

    /** Milliseconds before the first polling cycle fires after application startup. */
    private long initialDelayMs = 5_000;

    /** Maximum number of events claimed per polling cycle. */
    private int batchSize = 100;

    /**
     * Identifier recorded on each claim row in the outbox table, enabling operators to
     * correlate log output with specific worker instances and detect stale claims.
     * When blank, the auto-configuration generates a value from the local hostname plus
     * a random suffix so that each started JVM gets a unique but human-readable identity.
     */
    private String workerId = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(final long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(final long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(final String workerId) {
        this.workerId = workerId;
    }
}
