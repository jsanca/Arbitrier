package com.arbitrier.platform.spring;

import com.arbitrier.platform.messaging.outbox.mapper.DomainEventToOutboxMapper;
import com.arbitrier.platform.messaging.serialization.EventSerializer;
import com.arbitrier.platform.messaging.serialization.JacksonEventSerializer;
import com.arbitrier.platform.time.SystemClock;
import com.arbitrier.platform.time.TimeProvider;
import com.arbitrier.platform.web.CorrelationFilter;
import com.arbitrier.platform.web.PlatformExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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

    /**
     * Provides a dedicated {@link ObjectMapper} for messaging serialization.
     * Registered only when Spring Data JPA is on the classpath.
     * Override this bean to customize serialization modules (JavaTime, JSpecify, etc.)
     * across all messaging in the application.
     */
    @Bean
    @ConditionalOnMissingBean(name = "messagingObjectMapper")
    @ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
    public ObjectMapper messagingObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Provides the default Jackson-based {@link EventSerializer} used by the outbox
     * pipeline. Registered only when Spring Data JPA is on the classpath, which is
     * true for every persistence-bearing service.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
    public EventSerializer eventSerializer(ObjectMapper messagingObjectMapper) {
        return new JacksonEventSerializer(messagingObjectMapper);
    }

    /**
     * Provides the mapper that converts domain events into outbox records.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
    public DomainEventToOutboxMapper domainEventToOutboxMapper(EventSerializer eventSerializer,
                                                                TimeProvider timeProvider) {
        return new DomainEventToOutboxMapper(eventSerializer, timeProvider);
    }
}
