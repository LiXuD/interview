package com.interviewcoach.common.security;

import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT 认证过滤器，拦截每个请求，从 Authorization Bearer Token 中解析用户并设置到 SecurityContext。
 * Token 无效时静默跳过，继续以匿名身份执行后续过滤链。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * 拦截每个 HTTP 请求，尝试从 Authorization Bearer Token 中解析用户身份。
     * Token 无效或缺失时静默跳过，继续以匿名身份执行后续过滤链。
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头提取 Authorization Bearer Token
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // 2. 解析 token 获取用户 ID，查询数据库确认用户存在
                UUID userId = jwtTokenProvider.getUserId(token);
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    // 3. 用户有效，构建认证对象并设置到 SecurityContext，包含角色权限
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException ignored) {
                // 4. Token 无效或过期，跳过认证，继续以匿名身份处理请求
            }
        }
        // 5. 继续执行后续过滤链
        filterChain.doFilter(request, response);
    }
}
