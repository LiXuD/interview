package com.interviewcoach.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.error.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

/**
 * Spring Security 配置。
 * 禁用 CSRF、使用无状态会话、配置公开端点和 JWT 过滤器链。
 * 未认证请求统一返回 401 JSON 错误响应。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    /** 是否启用开发登录端点 */
    private final boolean devLoginEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper,
                          @Value("${app.auth.dev-login-enabled:false}") boolean devLoginEnabled) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.devLoginEnabled = devLoginEnabled;
    }

    /**
     * 配置安全过滤链：公开 health 和认证端点，其余请求需认证。
     * 未认证请求统一返回 401 JSON 错误响应。
     *
     * @param http Spring Security HttpSecurity 构建器
     * @return 配置完成的 SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF（REST API 使用 Bearer Token，无需 CSRF 保护）
                .csrf(csrf -> csrf.disable())
                // 2. 使用无状态会话，不创建 HttpSession
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 3. 配置端点授权规则
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/health").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/apple").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/wechat").permitAll();
                    if (devLoginEnabled) {
                        auth.requestMatchers(HttpMethod.POST, "/api/auth/dev-login").permitAll();
                    }
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                // 4. 在 UsernamePasswordAuthenticationFilter 之前插入 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 5. 配置未认证请求返回 401 JSON 响应
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ErrorResponse error = new ErrorResponse(
                                    "UNAUTHORIZED",
                                    "Authentication required",
                                    UUID.randomUUID().toString()
                            );
                            objectMapper.writeValue(response.getOutputStream(), error);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ErrorResponse error = new ErrorResponse(
                                    "ACCESS_DENIED",
                                    "Insufficient permissions",
                                    UUID.randomUUID().toString()
                            );
                            objectMapper.writeValue(response.getOutputStream(), error);
                        })
                );
        return http.build();
    }
}
