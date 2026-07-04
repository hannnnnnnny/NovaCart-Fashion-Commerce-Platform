package com.novacart.store.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${novacart.security.jwt-secret}") String jwtSecret,
            @Value("${novacart.security.jwt-expiration-minutes}") long expirationMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String subject, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String extractSubject(String token) {
        JwtTokenValidation validation = validateToken(token);
        return validation.status() == JwtTokenStatus.VALID ? validation.subject() : null;
    }

    public JwtTokenValidation validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String subject = claims.getSubject();
            return subject == null || subject.isBlank()
                    ? JwtTokenValidation.invalid()
                    : JwtTokenValidation.valid(subject);
        } catch (ExpiredJwtException exception) {
            return JwtTokenValidation.expired();
        } catch (JwtException | IllegalArgumentException exception) {
            return JwtTokenValidation.invalid();
        }
    }

    public boolean isTokenValid(String token, String expectedSubject) {
        String subject = extractSubject(token);
        return subject != null && subject.equalsIgnoreCase(expectedSubject);
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                // Tolerate small clock differences between issuer and verifier
                // so a token isn't spuriously rejected right at its boundary.
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
