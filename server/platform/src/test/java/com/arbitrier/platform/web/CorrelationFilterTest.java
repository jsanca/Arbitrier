package com.arbitrier.platform.web;

import com.arbitrier.platform.logging.StructuredLogFields;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies correlation propagation behaviour and, critically, that the filter never
 * touches W3C Trace Context headers ({@code traceparent}/{@code tracestate}) — those
 * are owned exclusively by the OpenTelemetry SDK (ADR-0008).
 */

class CorrelationFilterTest {

    private CorrelationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationFilter();
        MDC.clear();
    }

    @Test
    void provided_correlation_id_is_preserved() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationHeaders.CORRELATION_ID, "existing-cid");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationHeaders.CORRELATION_ID)).isEqualTo("existing-cid");
    }

    @Test
    void missing_correlation_id_generates_new_one() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationHeaders.CORRELATION_ID)).isNotBlank();
    }

    @Test
    void blank_correlation_id_generates_new_one() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationHeaders.CORRELATION_ID, "   ");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationHeaders.CORRELATION_ID)).isNotBlank();
        assertThat(response.getHeader(CorrelationHeaders.CORRELATION_ID).trim()).isNotEmpty();
    }

    @Test
    void request_id_is_always_generated() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationHeaders.REQUEST_ID)).isNotBlank();
    }

    @Test
    void two_requests_get_different_request_ids() throws Exception {
        MockHttpServletRequest  req1 = new MockHttpServletRequest();
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockHttpServletRequest  req2 = new MockHttpServletRequest();
        MockHttpServletResponse res2 = new MockHttpServletResponse();

        filter.doFilter(req1, res1, new MockFilterChain());
        filter.doFilter(req2, res2, new MockFilterChain());

        assertThat(res1.getHeader(CorrelationHeaders.REQUEST_ID))
                .isNotEqualTo(res2.getHeader(CorrelationHeaders.REQUEST_ID));
    }

    @Test
    void mdc_is_populated_during_chain_execution() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationHeaders.CORRELATION_ID, "trace-cid");

        String[] capturedCid = {null};
        String[] capturedRid = {null};

        FilterChain capturingChain = (req, res) -> {
            capturedCid[0] = MDC.get(StructuredLogFields.CORRELATION_ID);
            capturedRid[0] = MDC.get(StructuredLogFields.REQUEST_ID);
        };

        filter.doFilter(request, response, capturingChain);

        assertThat(capturedCid[0]).isEqualTo("trace-cid");
        assertThat(capturedRid[0]).isNotBlank();
    }

    @Test
    void mdc_is_cleared_after_successful_request() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(StructuredLogFields.CORRELATION_ID)).isNull();
        assertThat(MDC.get(StructuredLogFields.REQUEST_ID)).isNull();
    }

    @Test
    void mdc_is_cleared_even_when_chain_throws() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain throwingChain = (req, res) -> { throw new RuntimeException("downstream error"); };

        assertThatThrownBy(() -> filter.doFilter(request, response, throwingChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("downstream error");

        assertThat(MDC.get(StructuredLogFields.CORRELATION_ID)).isNull();
        assertThat(MDC.get(StructuredLogFields.REQUEST_ID)).isNull();
    }

    // ── W3C Trace Context — ADR-0008 ──────────────────────────────────────────

    @Test
    void filter_does_not_generate_traceparent_header() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceContextHeaders.TRACEPARENT)).isNull();
        assertThat(response.getHeader(TraceContextHeaders.TRACESTATE)).isNull();
    }

    @Test
    void filter_does_not_echo_or_overwrite_incoming_traceparent() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceContextHeaders.TRACEPARENT,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        request.addHeader(TraceContextHeaders.TRACESTATE, "vendor=value");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceContextHeaders.TRACEPARENT)).isNull();
        assertThat(response.getHeader(TraceContextHeaders.TRACESTATE)).isNull();
    }

    @Test
    void correlation_headers_work_alongside_traceparent() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceContextHeaders.TRACEPARENT,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        request.addHeader(CorrelationHeaders.CORRELATION_ID, "biz-correlation-123");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationHeaders.CORRELATION_ID)).isEqualTo("biz-correlation-123");
        assertThat(response.getHeader(CorrelationHeaders.REQUEST_ID)).isNotBlank();
    }

    @Test
    void mdc_does_not_contain_trace_context_keys_set_by_filter() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceContextHeaders.TRACEPARENT,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        String[] capturedTraceId = {null};
        FilterChain capturingChain = (req, res) ->
                capturedTraceId[0] = MDC.get(StructuredLogFields.TRACE_ID);

        filter.doFilter(request, response, capturingChain);

        // traceId in MDC is populated by OTel bridge, not by the correlation filter
        assertThat(capturedTraceId[0]).isNull();
    }
}
