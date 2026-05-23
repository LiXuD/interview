package com.interviewcoach.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
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

    public String verifyAndGetSub(String identityToken) {
        try {
            ensureKeysLoaded(false);
            Claims claims = Jwts.parser()
                    .verifyWith(resolveKey(identityToken))
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(servicesId)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Apple identity token: " + ex.getMessage(), ex);
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
            throw new IllegalArgumentException("Unknown Apple signing key: " + kid);
        }
        return key;
    }

    private String extractKid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Malformed JWT");
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        try {
            JsonNode header = objectMapper.readTree(headerJson);
            JsonNode kid = header.get("kid");
            if (kid == null || kid.asText().isEmpty()) {
                throw new IllegalArgumentException("JWT header missing kid");
            }
            return kid.asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JWT header", e);
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
                throw new IllegalArgumentException("Apple JWKS endpoint returned " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode keys = root.get("keys");
            if (keys == null || !keys.isArray()) {
                throw new IllegalArgumentException("Invalid JWKS response");
            }
            keyCache.clear();
            for (JsonNode jwk : keys) {
                String kid = jwk.get("kid").asText();
                PublicKey publicKey = parseEcPublicKey(jwk);
                keyCache.put(kid, publicKey);
            }
            cacheExpiry = Instant.now().plusSeconds(3600);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to fetch Apple JWKS: " + e.getMessage(), e);
        }
    }

    private PublicKey parseEcPublicKey(JsonNode jwk) {
        try {
            String crv = jwk.get("crv").asText();
            byte[] x = Base64.getUrlDecoder().decode(jwk.get("x").asText());
            byte[] y = Base64.getUrlDecoder().decode(jwk.get("y").asText());

            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec(mapCurve(crv)));

            ECPoint ecPoint = new ECPoint(
                    new BigInteger(1, x),
                    new BigInteger(1, y));
            ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, parameters.getParameterSpec(java.security.spec.ECParameterSpec.class));
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Apple EC public key", e);
        }
    }

    private String mapCurve(String crv) {
        return switch (crv) {
            case "P-256" -> "secp256r1";
            default -> throw new IllegalArgumentException("Unsupported curve: " + crv);
        };
    }
}
