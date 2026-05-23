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

@Component
public class AppleTokenVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final String servicesId;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile Instant cacheExpiry = Instant.MIN;

    public AppleTokenVerifier(@Value("${app.apple.services-id}") String servicesId,
                              ObjectMapper objectMapper) {
        this.servicesId = servicesId;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String verifyAndGetSub(String identityToken, String rawNonce) {
        try {
            ensureKeysLoaded(false);
            Claims claims = Jwts.parser()
                    .verifyWith(resolveKey(identityToken))
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(servicesId)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();

            if (rawNonce != null && !rawNonce.isBlank()) {
                String tokenNonce = claims.get("nonce", String.class);
                if (tokenNonce == null) {
                    throw new AppleAuthFailedException("Apple identity token missing nonce claim");
                }
                String hashedNonce = hashNonce(rawNonce);
                if (!tokenNonce.equals(hashedNonce)) {
                    throw new AppleAuthFailedException("Nonce mismatch");
                }
            }

            return claims.getSubject();
        } catch (AppleAuthFailedException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AppleAuthFailedException("Invalid Apple identity token: " + ex.getMessage(), ex);
        }
    }

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

    private String hashNonce(String nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(nonce.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AppleAuthFailedException("Failed to hash nonce", e);
        }
    }
}
