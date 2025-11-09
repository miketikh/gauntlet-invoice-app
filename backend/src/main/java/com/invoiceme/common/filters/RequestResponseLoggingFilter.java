package com.invoiceme.common.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter that logs HTTP requests and responses with timing information.
 * Excludes health check endpoints to reduce noise in logs.
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final List<String> EXCLUDED_PATHS = Arrays.asList("/actuator", "/health");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip logging for excluded paths
        String requestUri = request.getRequestURI();
        if (shouldExclude(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        logger.info("Request: {} {}", request.getMethod(), requestUri);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 500) {
                logger.error("Response: {} {} - Status: {} - Duration: {}ms",
                        request.getMethod(), requestUri, status, duration);
            } else if (status >= 400) {
                logger.warn("Response: {} {} - Status: {} - Duration: {}ms",
                        request.getMethod(), requestUri, status, duration);
            } else {
                logger.info("Response: {} {} - Status: {} - Duration: {}ms",
                        request.getMethod(), requestUri, status, duration);
            }
        }
    }

    private boolean shouldExclude(String requestUri) {
        return EXCLUDED_PATHS.stream().anyMatch(requestUri::startsWith);
    }
}
