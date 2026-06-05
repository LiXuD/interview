package com.interviewcoach.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.error.AppleAuthFailedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apple Sign in 身份令牌验证器。
 * 通过 Apple JWKS 端点获取公钥，验证 identityToken 的签名、issuer、audience 和 nonce。
 * 公钥缓存 1 小时，避免频繁请求 Apple 服务器。
 */
@Component
public class AppleTokenVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    /** Apple 预期的 issuer 值 */
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final String servicesId;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /** Apple JWKS 公钥缓存，key 为 kid */
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    /** 公钥缓存过期时间 */
    private volatile Instant cacheExpiry = Instant.MIN;

    public AppleTokenVerifier(@Value("${app.apple.services-id}") String servicesId,
                              ObjectMapper objectMapper) {
        this.servicesId = servicesId;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 验证 Apple identity token 并返回用户的 subject（appleUserId）。
     *
     * @param identityToken Apple 返回的 identity token
     * @param rawNonce      iOS 客户端生成的原始 nonce
     * @return token 中的 subject（appleUserId）
     * @throws AppleAuthFailedException 验证失败时抛出
     */
    public String verifyAndGetSub(String identityToken, String rawNonce) {
        try {
            // 1. 确保 Apple JWKS 公钥已加载到缓存（过期则自动刷新）
            ensureKeysLoaded(false);
            // 2. 根据 token header 中的 kid 解析对应公钥，验证签名、issuer 和 audience
            Claims claims = Jwts.parser()
                    .verifyWith(resolveKey(identityToken))
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(servicesId)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            // 3. 校验 token 中的 nonce 与客户端传入的 rawNonce 的 SHA-256 哈希是否一致
            String tokenNonce = claims.get("nonce", String.class);
            if (tokenNonce == null) {
                throw new AppleAuthFailedException("Apple identity token missing nonce claim");
            }
            String hashedNonce = hashNonce(rawNonce);
            if (!tokenNonce.equals(hashedNonce)) {
                throw new AppleAuthFailedException("Nonce mismatch");
            }

            // 4. 验证通过，返回 subject（appleUserId）
            return claims.getSubject();
        } catch (AppleAuthFailedException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AppleAuthFailedException("Invalid Apple identity token: " + ex.getMessage(), ex);
        }
    }

    /** 根据 token header 中的 kid 从缓存中解析对应的公钥 */
    private PublicKey resolveKey(String token) {
        String kid = extractKid(token);
        PublicKey key = keyCache.get(kid);
        if (key == null) {
            ensureKeysLoaded(true);
            key = keyCache.get(kid);
        }
        if (key == null) {
            throw new AppleAuthFailedException("Unknown Apple signing key: " + kid);
        }
        return key;
    }

    /** 从 JWT header 中提取 kid（密钥 ID） */
    private String extractKid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new AppleAuthFailedException("Malformed JWT");
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        try {
            JsonNode header = objectMapper.readTree(headerJson);
            JsonNode kid = header.get("kid");
            if (kid == null || kid.asText().isEmpty()) {
                throw new AppleAuthFailedException("JWT header missing kid");
            }
            return kid.asText();
        } catch (AppleAuthFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new AppleAuthFailedException("Failed to parse JWT header", e);
        }
    }

    /** 从 Apple JWKS 端点加载公钥，支持强制刷新和缓存过期检查 */
    private synchronized void ensureKeysLoaded(boolean force) {
        if (!force && Instant.now().isBefore(cacheExpiry) && !keyCache.isEmpty()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(APPLE_JWKS_URL))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AppleAuthFailedException("Apple JWKS endpoint returned " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode keys = root.get("keys");
            if (keys == null || !keys.isArray()) {
                throw new AppleAuthFailedException("Invalid JWKS response");
            }
            keyCache.clear();
            for (JsonNode jwk : keys) {
                String kid = jwk.get("kid").asText();
                PublicKey publicKey = parseRsaPublicKey(jwk);
                keyCache.put(kid, publicKey);
            }
            cacheExpiry = Instant.now().plusSeconds(3600);
        } catch (AppleAuthFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new AppleAuthFailedException("Failed to fetch Apple JWKS: " + e.getMessage(), e);
        }
    }

    /** 将 JWK 格式的 RSA 公钥参数解析为 Java PublicKey */
    private PublicKey parseRsaPublicKey(JsonNode jwk) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(jwk.get("n").asText());
            byte[] eBytes = Base64.getUrlDecoder().decode(jwk.get("e").asText());

            RSAPublicKeySpec spec = new RSAPublicKeySpec(
                    new BigInteger(1, nBytes),
                    new BigInteger(1, eBytes));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new AppleAuthFailedException("Failed to parse Apple RSA public key", e);
        }
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** 对原始 nonce 执行 SHA-256 哈希，返回十六进制字符串 */
    private String hashNonce(String nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(nonce.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[hash.length * 2];
            for (int i = 0; i < hash.length; i++) {
                hex[i * 2] = HEX[(hash[i] >> 4) & 0x0F];
                hex[i * 2 + 1] = HEX[hash[i] & 0x0F];
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available", e);
        }
    }
}
