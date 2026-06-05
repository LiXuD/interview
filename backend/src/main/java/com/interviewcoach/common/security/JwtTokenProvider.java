package com.interviewcoach.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌的签发与验证工具，基于 HMAC-SHA 密钥。
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    /** 令牌过期时间（毫秒） */
    private final long expirationMs;

    /**
     * 初始化 JWT 令牌提供者，从 Base64 编码的密钥字符串解码 HMAC-SHA 签名密钥。
     *
     * @param secret        Base64 编码的 HMAC-SHA 密钥
     * @param expirationMs  令牌过期时间（毫秒）
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * 为指定用户签发 JWT 令牌。
     *
     * @param userId 用户 ID
     * @return 签发的 JWT 字符串
     */
    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 验证 JWT 令牌并返回解析后的 Claims。
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims
     * @throws JwtException 令牌无效或过期
     */
    public Claims validateToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 JWT 令牌中提取用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     * @throws JwtException 令牌无效
     */
    public UUID getUserId(String token) {
        Claims claims = validateToken(token);
        return UUID.fromString(claims.getSubject());
    }
}
