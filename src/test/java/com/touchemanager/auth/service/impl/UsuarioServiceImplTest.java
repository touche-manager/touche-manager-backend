package com.touchemanager.auth.service.impl;

import com.touchemanager.auth.dto.*;
import com.touchemanager.auth.entity.NombreRol;
import com.touchemanager.auth.entity.Rol;
import com.touchemanager.auth.entity.Usuario;
import com.touchemanager.auth.repository.RolRepository;
import com.touchemanager.auth.repository.UsuarioRepository;
import com.touchemanager.auth.service.JwtService;
import com.touchemanager.shared.exception.EmailYaExisteException;
import com.touchemanager.shared.exception.InvalidCredentialsException;
import com.touchemanager.shared.exception.RolNoAsignadoException;
import com.touchemanager.shared.exception.RolNoEncontradoException;
import com.touchemanager.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

    @Mock
    private JwtService jwtService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private RegisterRequestDTO request;
    private Rol rolAtleta;
    private Rol rolOrganizador;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        request = new RegisterRequestDTO();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setRoles(Set.of(NombreRol.ATLETA));

        rolAtleta = new Rol();
        rolAtleta.setId(1L);
        rolAtleta.setNombre(NombreRol.ATLETA);

        rolOrganizador = new Rol();
        rolOrganizador.setId(2L);
        rolOrganizador.setNombre(NombreRol.ORGANIZADOR);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("test@test.com");
        usuario.setPassword("encodedPassword");
        usuario.setActivo(true);
        usuario.setRoles(Set.of(rolAtleta));
    }

    @Test
    void registrar_Exito() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ATLETA)).thenReturn(Optional.of(rolAtleta));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        RegisterResponseDTO response = usuarioService.registrar(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@test.com", response.email());
        assertTrue(response.roles().contains(NombreRol.ATLETA));

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void registrar_EmailYaExiste() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(EmailYaExisteException.class, () -> usuarioService.registrar(request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void registrar_RolNoEncontrado() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findByNombre(NombreRol.ATLETA)).thenReturn(Optional.empty());
        assertThrows(RolNoEncontradoException.class, () -> usuarioService.registrar(request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void login_ExitoSingleRole() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(1L, "test@test.com", NombreRol.ATLETA)).thenReturn("mockedToken");

        LoginResponseDTO response = usuarioService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mockedToken", response.token());
        assertNull(response.roles());
    }

    @Test
    void login_ExitoMultipleRoles() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");

        usuario.setRoles(Set.of(rolAtleta, rolOrganizador));

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        LoginResponseDTO response = usuarioService.login(loginRequest);

        assertNotNull(response);
        assertNull(response.token());
        assertNotNull(response.roles());
        assertEquals(2, response.roles().size());
        assertTrue(response.roles().contains(NombreRol.ATLETA));
        assertTrue(response.roles().contains(NombreRol.ORGANIZADOR));
    }

    @Test
    void login_CredencialesInvalidas() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("wrongPassword");

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> usuarioService.login(loginRequest));
    }

    @Test
    void login_UsuarioInactivo() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");

        usuario.setActivo(false);
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(usuario));

        assertThrows(InvalidCredentialsException.class, () -> usuarioService.login(loginRequest));
    }

    @Test
    void selectRole_Exito() {
        SelectRoleRequestDTO selectRoleRequest = new SelectRoleRequestDTO();
        selectRoleRequest.setRol(NombreRol.ATLETA);

        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(1L, "test@test.com", NombreRol.ATLETA)).thenReturn("mockedToken");

        LoginResponseDTO response = usuarioService.selectRole("test@test.com", selectRoleRequest);

        assertNotNull(response);
        assertEquals("mockedToken", response.token());
        assertNull(response.roles());
    }

    @Test
    void selectRole_RolNoAsignado() {
        SelectRoleRequestDTO selectRoleRequest = new SelectRoleRequestDTO();
        selectRoleRequest.setRol(NombreRol.ORGANIZADOR);

        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));

        assertThrows(RolNoAsignadoException.class, () -> usuarioService.selectRole("test@test.com", selectRoleRequest));
    }

    @Test
    void getProfile_Exito() {
        usuario.setProfilePictureKey("profile-pictures/1/pic.png");
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));

        UserProfileDTO profile = usuarioService.getProfile("test@test.com");

        assertNotNull(profile);
        assertEquals(usuario.getId(), profile.id());
        assertEquals(usuario.getEmail(), profile.email());
        assertEquals("/api/users/profile-picture/1", profile.profilePictureUrl());
    }

    @Test
    void uploadProfilePicture_Exito() {
        MockMultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));
        when(fileStorageService.uploadFile(any(), anyString())).thenReturn("profile-pictures/1/new_pic.png");

        UserProfileDTO profile = usuarioService.uploadProfilePicture("test@test.com", file);

        assertNotNull(profile);
        assertEquals("/api/users/profile-picture/1", profile.profilePictureUrl());
        verify(fileStorageService, times(1)).uploadFile(file, "profile-pictures/1");
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void deleteProfilePicture_Exito() {
        usuario.setProfilePictureKey("profile-pictures/1/pic.png");
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));

        UserProfileDTO profile = usuarioService.deleteProfilePicture("test@test.com");

        assertNotNull(profile);
        assertNull(profile.profilePictureUrl());
        verify(fileStorageService, times(1)).deleteFile("profile-pictures/1/pic.png");
        assertNull(usuario.getProfilePictureKey());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void getProfilePicture_Exito() {
        usuario.setProfilePictureKey("profile-pictures/1/pic.png");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(fileStorageService.downloadFile("profile-pictures/1/pic.png")).thenReturn(new ByteArrayInputStream("data".getBytes()));

        InputStream stream = usuarioService.getProfilePicture(1L);

        assertNotNull(stream);
        verify(fileStorageService, times(1)).downloadFile("profile-pictures/1/pic.png");
    }

    @Test
    void getProfilePictureContentType_Exito() {
        usuario.setProfilePictureKey("profile-pictures/1/pic.png");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        String contentType = usuarioService.getProfilePictureContentType(1L);

        assertEquals("image/png", contentType);
    }
}
