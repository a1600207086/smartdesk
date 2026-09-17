package com.smartdesk.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("SMARTDESK_JWT_SECRET must contain at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public IssuedToken issue(UserEntity user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        String jwtId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(jwtId)
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("tenantId", user.getTenantId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, jwtId, expiresAt);
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number tenantId = (Number) claims.get("tenantId");
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                tenantId.longValue(),
                claims.get("username", String.class),
                UserRole.valueOf(claims.get("role", String.class)),
                claims.getId(),
                claims.getExpiration().toInstant()
        );
    }

    public record IssuedToken(
            String value,
            String jwtId,
            Instant expiresAt
    ) {
    }
}