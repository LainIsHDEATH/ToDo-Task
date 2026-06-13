package ua.ivan.todo.tasks.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Slf4j
@Order(2)
public class RestCallLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUEST_BODY_LENGTH = 10_000;

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password",
        "oldPassword",
        "newPassword",
        "passwordHash",
        "accessToken",
        "token");

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest =
            new ContentCachingRequestWrapper(request, MAX_REQUEST_BODY_LENGTH);
        ContentCachingResponseWrapper wrappedResponse =
            new ContentCachingResponseWrapper(response);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        long startTime = System.currentTimeMillis();

        logRequestStarted(method, uri, query);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status = wrappedResponse.getStatus();

            String requestBody = getRequestBody(wrappedRequest);
            String responseBody = getResponseBody(wrappedResponse);

            logRequestBody(method, uri, requestBody);
            logResponse(method, uri, status, durationMs, responseBody);

            wrappedResponse.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return uri.startsWith("/actuator")
            || uri.startsWith("/swagger-ui")
            || uri.startsWith("/v3/api-docs");
    }

    private void logRequestStarted(String method, String uri, String query) {
        log.info("REST request started. method={}, uri={}, query={}", method, uri, query);
    }

    private void logRequestBody(String method, String uri, String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return;
        }

        log.info("REST request body. method={}, uri={}, body={}",
            method, uri, maskSensitiveData(requestBody));
    }

    private void logResponse(
        String method,
        String uri,
        int status,
        long durationMs,
        String responseBody) {
        if (status >= 400) {
            log.warn("REST response error. method={}, uri={}, status={}, durationMs={}, body={}",
                method, uri, status, durationMs, maskSensitiveData(responseBody));
            return;
        }

        log.info("REST response. method={}, uri={}, status={}, durationMs={}",
            method, uri, status, durationMs);
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();

        if (content.length == 0) {
            return null;
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();

        if (content.length == 0) {
            return null;
        }

        return new String(content, StandardCharsets.UTF_8);
    }

    private String maskSensitiveData(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        String maskedBody = body;

        for (String field : SENSITIVE_FIELDS) {
            maskedBody = maskedBody.replaceAll(
                "(\"" + field + "\"\\s*:\\s*\")([^\"]+)(\")",
                "$1***$3");
        }

        return maskedBody;
    }
}