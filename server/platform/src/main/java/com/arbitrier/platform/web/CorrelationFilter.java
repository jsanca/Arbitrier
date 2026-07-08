package com.arbitrier.platform.web;

import com.arbitrier.platform.correlation.CorrelationId;
import com.arbitrier.platform.correlation.RequestId;
import com.arbitrier.platform.logging.StructuredLogFields;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Servlet filter that populates MDC with {@code correlationId} and {@code requestId}.
 *
 * <p>If the incoming request carries an {@value CorrelationHeaders#CORRELATION_ID} header the
 * value is reused; otherwise a new {@link CorrelationId} is generated. A fresh
 * {@link RequestId} is always generated per request.
 *
 * <p>Both IDs are echoed back as response headers and removed from MDC in the {@code finally}
 * block so that subsequent requests start with a clean context, even when the filter chain
 * throws.
 *
 * <p>Layer: platform/web
 * <p>Module: platform
 */
public class CorrelationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String correlationId = request.getHeader(CorrelationHeaders.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = CorrelationId.generate().value();
        }
        String requestId = RequestId.generate().value();

        MDC.put(StructuredLogFields.CORRELATION_ID, correlationId);
        MDC.put(StructuredLogFields.REQUEST_ID, requestId);

        response.setHeader(CorrelationHeaders.CORRELATION_ID, correlationId);
        response.setHeader(CorrelationHeaders.REQUEST_ID, requestId);

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(StructuredLogFields.CORRELATION_ID);
            MDC.remove(StructuredLogFields.REQUEST_ID);
        }
    }

    @Override
    public void destroy() {
    }
}
