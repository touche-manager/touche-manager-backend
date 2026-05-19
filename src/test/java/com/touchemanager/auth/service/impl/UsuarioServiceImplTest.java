package com.touchemanager.auth.service.impl;

import com.touchemanager.auth.dto.RegisterRequestDTO;
import com.touchemanager.auth.dto.RegisterResponseDTO;
import com.touchemanager.auth.entity.NombreRol;
import com.touchemanager.auth.entity.Rol;
import com.touchemanager.auth.entity.Usuario;
import com.touchemanager.auth.repository.RolRepository;
import com.touchemanager.auth.repository.UsuarioRepository;
import com.touchemanager.shared.exception.EmailYaExisteException;
import com.touchemanager.shared.exception.RolNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private RegisterRequestDTO request;
    private Rol rolAtleta;

    @BeforeEach
    void setUp() {
        request = new RegisterRequestDTO();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setRoles(Set.of(NombreRol.ATLETA));

        rolAtleta = new Rol();
        rolAtleta.setId(1L);
        rolAtleta.setNombre(NombreRol.ATLETA);
    }

    @Test
    void registrar_Exito() {
        // Arrange
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ATLETA)).thenReturn(Optional.of(rolAtleta));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        Usuario usuarioGuardado = new Usuario();
        usuarioGuardado.setId(1L);
        usuarioGuardado.setEmail("test@test.com");
        usuarioGuardado.setPassword("encodedPassword");
        usuarioGuardado.setRoles(Set.of(rolAtleta));

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        // Act
        RegisterResponseDTO response = usuarioService.registrar(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@test.com", response.email());
        assertTrue(response.roles().contains(NombreRol.ATLETA));

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void registrar_EmailYaExiste() {
        // Arrange
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailYaExisteException.class, () -> usuarioService.registrar(request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void registrar_RolNoEncontrado() {
        // Arrange
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ATLETA)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RolNoEncontradoException.class, () -> usuarioService.registrar(request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
