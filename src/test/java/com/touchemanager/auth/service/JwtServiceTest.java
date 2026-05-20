package com.touchemanager.auth.service;

import com.touchemanager.auth.entity.NombreRol;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "mySecretKeyForTestingPurposesThatIsAtLeast32BytesLong!";
    private final long expirationMs = 3600000; // 1 hora

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    void generateToken_WithRol_Success() {
        String token = jwtService.generateToken(1L, "user@test.com", NombreRol.ATLETA);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals("user@test.com", jwtService.extractEmail(token));
        assertEquals(NombreRol.ATLETA.name(), jwtService.extractRol(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void generateToken_WithoutRol_Success() {
        String token = jwtService.generateToken(2L, "admin@test.com", null);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals("admin@test.com", jwtService.extractEmail(token));
        assertNull(jwtService.extractRol(token));
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractClaims_Success() {
        String token = jwtService.generateToken(1L, "user@test.com", NombreRol.ATLETA);

        Claims claims = jwtService.extractAllClaims(token);
        assertNotNull(claims);
        assertEquals("user@test.com", claims.getSubject());
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals(NombreRol.ATLETA.name(), claims.get("rol", String.class));
    }

    @Test
    void isTokenValid_ExpiredToken() {
        // Creamos un JwtService con tiempo de expiración negativo para que expire inmediatamente
        JwtService expiredJwtService = new JwtService(secret, -1000);
        String token = expiredJwtService.generateToken(1L, "expired@test.com", NombreRol.ATLETA);

        assertFalse(expiredJwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_InvalidSignature() {
        String token = jwtService.generateToken(1L, "user@test.com", NombreRol.ATLETA);

        // Creamos otro JwtService con una firma secreta distinta
        String differentSecret = "anotherSecretKeyForTestingPurposesThatIsAtLeast32BytesLong!";
        JwtService differentJwtService = new JwtService(differentSecret, expirationMs);

        assertFalse(differentJwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_MalformedToken() {
        assertFalse(jwtService.isTokenValid("malformed.token.here"));
    }
}
