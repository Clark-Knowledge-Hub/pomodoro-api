package com.pomodoro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final String deviceKey;
    private final String frontendKey;

    public ApiKeyAuthFilter(String deviceKey, String frontendKey) {
        this.deviceKey = deviceKey;
        this.frontendKey = frontendKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        if (requestApiKey == null || requestApiKey.isBlank()) {
            writeError(response, "Missing API Key", "Header X-API-KEY is required");
            return;
        }

        if (deviceKey.equals(requestApiKey)) {
            var auth = new ApiKeyAuthToken(
                    requestApiKey,
                    "pico-w-device",
                    AuthorityUtils.createAuthorityList("ROLE_DEVICE")
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        if (frontendKey.equals(requestApiKey)) {
            var auth = new ApiKeyAuthToken(
                    requestApiKey,
                    "frontend",
                    AuthorityUtils.createAuthorityList("ROLE_FRONTEND")
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        writeError(response, "Invalid API Key", "The provided API Key is not valid");
    }

    private void writeError(HttpServletResponse response, String error, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"status\":401,\"error\":\"%s\",\"message\":\"%s\"}", error, message)
        );
    }
}

