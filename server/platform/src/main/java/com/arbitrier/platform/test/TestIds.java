package com.arbitrier.platform.test;

import com.arbitrier.platform.correlation.CausationId;
import com.arbitrier.platform.correlation.CorrelationId;
import com.arbitrier.platform.correlation.MessageId;
import com.arbitrier.platform.correlation.RequestId;
import com.arbitrier.platform.idempotency.IdempotencyKey;

/**
 * Factory for generating typed test identifiers.
 *
 * <p>Use in unit and integration tests instead of constructing raw UUIDs so that
 * test intent is clear and IDs carry their domain type.
 *
 * <p>Example:
 * <pre>{@code
 * CorrelationId id = TestIds.correlationId();
 * }</pre>
 */
public final class TestIds {

    private TestIds() {
    }

    /** @return a new random {@link CorrelationId} */
    public static CorrelationId correlationId() {
        return CorrelationId.generate();
    }

    /** @return a new random {@link CausationId} */
    public static CausationId causationId() {
        return CausationId.generate();
    }

    /** @return a new random {@link MessageId} */
    public static MessageId messageId() {
        return MessageId.generate();
    }

    /** @return a new random {@link RequestId} */
    public static RequestId requestId() {
        return RequestId.generate();
    }

    /** @return a new random {@link IdempotencyKey} */
    public static IdempotencyKey idempotencyKey() {
        return IdempotencyKey.generate();
    }
}
