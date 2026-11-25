package com.hcl.Order_Management_System.Security;

import com.hcl.Order_Management_System.Model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secretBytes;
    private final long expirationMs;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.jwt.secret:}") String configuredSecret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
            ObjectMapper objectMapper) {
        this.expirationMs = expirationMs;
        this.objectMapper = objectMapper;
        this.secretBytes = resolveSecretBytes(configuredSecret);
    }

    public String generateToken(User user) {
        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + Math.max(60, expirationMs / 1000);

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("uid", user.getUserId());
        payload.put("role", user.getRole().name());
        payload.put("iat", nowSeconds);
        payload.put("exp", expSeconds);

        String encodedHeader = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
        String encodedPayload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = sign(signingInput);
        return signingInput + "." + signature;
    }

    public JwtClaims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = sign(signingInput);
        if (!MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        try {
            byte[] payloadBytes = URL_DECODER.decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(
                    payloadBytes,
                    new TypeReference<Map<String, Object>>() {});

            Object subObj = claims.get("sub");
            Object expObj = claims.get("exp");
            Object roleObj = claims.get("role");
            if (!(subObj instanceof String username) || !(expObj instanceof Number expNum)) {
                return null;
            }

            long expEpochSeconds = expNum.longValue();
            if (Instant.now().getEpochSecond() >= expEpochSeconds) {
                return null;
            }

            return new JwtClaims(username, roleObj != null ? roleObj.toString() : "");
        } catch (Exception ex) {
            return null;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] rawSig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(rawSig);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign JWT token.", ex);
        }
    }

    private byte[] resolveSecretBytes(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            byte[] bytes = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
            if (bytes.length >= 32) {
                return bytes;
            }
            try {
                return MessageDigest.getInstance("SHA-256").digest(bytes);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to derive JWT key.", ex);
            }
        }
        // Generate an ephemeral 256-bit key when no external secret is configured (avoids hardcoded secrets)
        byte[] ephemeral = new byte[32];
        new SecureRandom().nextBytes(ephemeral);
        return ephemeral;
    }

    public record JwtClaims(String username, String role) {
    }
}
