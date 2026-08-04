package com.globallogic.bloodbridge.auth.security;

import com.globallogic.bloodbridge.auth.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and (locally, for tests/tools) validates signed JWTs. The same
 * shared secret is configured on the API Gateway, which is the service
 * that actually enforces validation on every protected request (added in
 * the commit that introduces donor-service) — this keeps auth stateless
 * and avoids a network round-trip per request.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${bloodbridge.jwt.secret}") String secret,
            @Value("${bloodbridge.jwt.expiration-seconds:3600}") long expirationSeconds) {
        // HS256 requires a key of at least 256 bits (32 bytes); the default
        // dev secret in application.yml is padded well beyond that.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationSeconds * 1000;
    }

    public String generateToken(Long userId, String email, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    /** Used by AuthServiceTest and any internal tooling; the Gateway does its own copy of this check. */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
