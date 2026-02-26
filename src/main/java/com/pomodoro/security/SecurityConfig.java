package com.pomodoro.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${api.security.device-key}")
    private String deviceKey;

    @Value("${api.security.frontend-key}")
    private String frontendKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/sessions").hasRole("DEVICE")
                        .requestMatchers(HttpMethod.GET, "/sessions", "/sessions/**").hasAnyRole("DEVICE", "FRONTEND")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new ApiKeyAuthFilter(deviceKey, frontendKey), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

