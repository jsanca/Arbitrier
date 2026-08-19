package com.arbitrier.platform.correlation;

import java.util.Optional;

/**
 * Thread-local carrier for the active {@link CorrelationContext}.
 *
 * <p>Execution entry points (HTTP filters, Kafka consumers, schedulers) establish the context
 * before processing begins and clear it in a {@code finally} block after processing ends.
 * Business code reads the context through {@link #get()}.
 *
 * <p>The holder uses a plain {@code ThreadLocal} — not {@code InheritableThreadLocal} — so
 * context does not propagate automatically to child threads or {@code CompletionStage}
 * callbacks. Cross-thread propagation is addressed by future platform slices.
 *
 * <p>Layer: correlation
 * <p>Module: platform
 */
public final class CorrelationContextHolder {

    private static final ThreadLocal<CorrelationContext> holder = new ThreadLocal<>();

    private CorrelationContextHolder() {
    }

    /**
     * Set the correlation context for the current thread.
     *
     * @param context the context to install; must not be null
     */
    public static void set(final CorrelationContext context) {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        holder.set(context);
    }

    /**
     * Return the current thread's correlation context, if one has been installed.
     *
     * @return an {@link Optional} containing the active context, or empty when no context
     *         has been established for this thread
     */
    public static Optional<CorrelationContext> get() {
        return Optional.ofNullable(holder.get());
    }

    /**
     * Clear the correlation context for the current thread.
     *
     * <p>Must be called in the {@code finally} block of any entry point that called
     * {@link #set(CorrelationContext)}, to prevent context leakage on thread pool reuse.
     */
    public static void clear() {
        holder.remove();
    }
}
