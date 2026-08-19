package com.arbitrier.platform.web;

import com.arbitrier.platform.correlation.CorrelationContext;
import com.arbitrier.platform.correlation.CorrelationContextHolder;
import com.arbitrier.platform.correlation.CorrelationId;
import com.arbitrier.platform.correlation.DefaultCorrelationContext;
import com.arbitrier.platform.correlation.MdcCorrelationContextBridge;
import com.arbitrier.platform.correlation.RequestId;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet filter that establishes a {@link CorrelationContext} for each HTTP request and
 * bridges it to SLF4J MDC via {@link MdcCorrelationContextBridge}.
 *
 * <p>If the incoming request carries an {@value CorrelationHeaders#CORRELATION_ID} header the
 * value is reused; otherwise a new {@link CorrelationId} is generated. A fresh
 * {@link RequestId} is always generated per request.
 *
 * <p>Both IDs are echoed back as response headers. The context is cleared from the
 * {@link CorrelationContextHolder} and MDC in the {@code finally} block so that subsequent
 * requests start with a clean context, even when the filter chain throws.
 *
 * <p>Layer: platform/web
 * <p>Module: platform
 */
public class CorrelationFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res,
                         final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest  request  = (HttpServletRequest)  req;
        final HttpServletResponse response = (HttpServletResponse) res;

        String correlationId = request.getHeader(CorrelationHeaders.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = CorrelationId.generate().value();
        }
        final String requestId = RequestId.generate().value();

        final CorrelationContext context = new DefaultCorrelationContext(correlationId, requestId);
        CorrelationContextHolder.set(context);
        MdcCorrelationContextBridge.bind(context);

        response.setHeader(CorrelationHeaders.CORRELATION_ID, correlationId);
        response.setHeader(CorrelationHeaders.REQUEST_ID, requestId);

        try {
            chain.doFilter(req, res);
        } finally {
            MdcCorrelationContextBridge.unbind();
            CorrelationContextHolder.clear();
        }
    }

    @Override
    public void destroy() {
    }
}
