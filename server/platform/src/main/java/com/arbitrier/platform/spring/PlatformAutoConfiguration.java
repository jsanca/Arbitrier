package com.arbitrier.platform.spring;

import com.arbitrier.platform.time.SystemClock;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.web.CorrelationFilter;
import com.arbitrier.platform.web.PlatformExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Auto-configuration that activates platform web infrastructure in servlet web applications.
 *
 * <p>Registers:
 * <ul>
 *   <li>{@link CorrelationFilter} at {@link Ordered#HIGHEST_PRECEDENCE}</li>
 *   <li>{@link PlatformExceptionHandler}</li>
 *   <li>{@link TimeProvider} (SystemClock) when no other {@code TimeProvider} bean exists</li>
 * </ul>
 *
 * <p>Services may override the {@link TimeProvider} bean (e.g. in tests with
 * {@link com.arbitrier.platform.time.FixedTimeProvider}).
 *
 * <p>Layer: platform/spring
 * <p>Module: platform
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PlatformAutoConfiguration {

    /** Registers the correlation filter at the highest precedence. */
    @Bean
    public FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        FilterRegistrationBean<CorrelationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("correlationFilter");
        return registration;
    }

    /** Registers the platform-wide exception handler. */
    @Bean
    public PlatformExceptionHandler platformExceptionHandler() {
        return new PlatformExceptionHandler();
    }

    /**
     * Provides a real-clock {@link TimeProvider}.
     * Override by declaring your own {@code TimeProvider} bean in a service's
     * {@code config/} package or {@code @TestConfiguration}.
     */
    @Bean
    @ConditionalOnMissingBean
    public TimeProvider timeProvider() {
        return SystemClock.INSTANCE;
    }
}
