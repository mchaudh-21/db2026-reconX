package com.dbtraining.reconx.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Adds request correlation information to the logging MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String TRADE_REF_HEADER = "X-Trade-Ref";

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String correlationId = httpRequest.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String tradeRef = httpRequest.getHeader(TRADE_REF_HEADER);

        try {
            MDC.put("correlationId", correlationId);

            if (tradeRef != null && !tradeRef.isBlank()) {
                MDC.put("tradeRef", tradeRef);
            }

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
