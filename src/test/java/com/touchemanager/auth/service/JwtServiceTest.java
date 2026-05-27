package com.touchemanager.auth.service;

import com.touchemanager.auth.entity.RoleName;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "mySecretKeyForTestingPurposesThatIsAtLeast32BytesLong!";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    void generateToken_WithRole_Success() {
        String token = jwtService.generateToken(1L, "user@test.com", RoleName.ATHLETE);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals("user@test.com", jwtService.extractEmail(token));
        assertEquals(RoleName.ATHLETE.name(), jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void generateToken_WithoutRole_Success() {
        String token = jwtService.generateToken(2L, "admin@test.com", null);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals("admin@test.com", jwtService.extractEmail(token));
        assertNull(jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractClaims_Success() {
        String token = jwtService.generateToken(1L, "user@test.com", RoleName.ATHLETE);

        Claims claims = jwtService.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("user@test.com", claims.getSubject());
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals(RoleName.ATHLETE.name(), claims.get("role", String.class));
    }

    @Test
    void isTokenValid_ExpiredToken() {
        JwtService expiredJwtService = new JwtService(secret, -1000);
        String token = expiredJwtService.generateToken(1L, "expired@test.com", RoleName.ATHLETE);

        assertFalse(expiredJwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_InvalidSignature() {
        String token = jwtService.generateToken(1L, "user@test.com", RoleName.ATHLETE);

        String differentSecret = "anotherSecretKeyForTestingPurposesThatIsAtLeast32BytesLong!";
        JwtService differentJwtService = new JwtService(differentSecret, expirationMs);

        assertFalse(differentJwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_MalformedToken() {
        assertFalse(jwtService.isTokenValid("malformed.token.here"));
    }
}
