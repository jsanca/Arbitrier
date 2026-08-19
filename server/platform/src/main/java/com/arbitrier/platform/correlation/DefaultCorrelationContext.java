package com.arbitrier.platform.correlation;

import com.arbitrier.platform.validation.Require;

/**
 * Default immutable implementation of {@link CorrelationContext}.
 *
 * <p>Used by {@code CorrelationFilter} for HTTP-initiated execution and by future
 * scheduler and messaging adapters that establish their own correlation context.
 *
 * @param correlationId the correlation identifier; must not be blank
 * @param requestId     the per-request identifier; may be {@code null} for non-HTTP execution models
 *
 * <p>Layer: correlation
 * <p>Module: platform
 */
public record DefaultCorrelationContext(String correlationId, String requestId)
        implements CorrelationContext {

    public DefaultCorrelationContext {
        Require.notBlank(correlationId, "correlationId");
    }
}
