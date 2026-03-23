package com.timecom.sessiontracker.security;

import com.timecom.sessiontracker.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT Token Provider for generating, validating, and parsing JWT tokens.
 * Uses HS512 algorithm with a configurable secret key.
 * Includes an in-memory token blacklist for logout support.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshWindowMs;

    /** In-memory blacklist for revoked tokens (production: use Redis) */
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-window-ms}") long refreshWindowMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.refreshWindowMs = refreshWindowMs;
    }

    /**
     * Generate a JWT token for an authenticated user.
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername());
    }

    /**
     * Generate a JWT token for a username.
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Extract the username from a JWT token.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validate a JWT token.
     */
    public boolean validateToken(String token) {
        try {
            if (blacklistedTokens.contains(token)) {
                log.debug("Token is blacklisted");
                return false;
            }
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT token expired: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.debug("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.debug("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if a token is within the refresh window (near expiry).
     */
    public boolean isTokenRefreshable(String token) {
        try {
            Claims claims = parseClaims(token);
            long timeToExpiry = claims.getExpiration().getTime() - System.currentTimeMillis();
            return timeToExpiry > 0 && timeToExpiry <= refreshWindowMs;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Blacklist a token (e.g., on logout).
     */
    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    /**
     * Check if a token is blacklisted.
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    /**
     * Get the token expiration time in milliseconds.
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
